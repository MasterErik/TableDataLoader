package su.erik.tabledataloader.importer.csv;

import com.opencsv.bean.*;
import com.opencsv.enums.CSVReaderNullFieldIndicator;
import su.erik.tabledataloader.config.Constant;
import su.erik.tabledataloader.importer.AbstractFileImporter;
import su.erik.tabledataloader.importer.EncodingDetector;
import su.erik.tabledataloader.importer.ImportMapper;
import su.erik.tabledataloader.importer.annotation.DynamicColumn;
import su.erik.tabledataloader.importer.csv.strategy.DynamicColumnMappingStrategy;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * Реализация импортера для CSV файлов.
 * Использует OpenCSV и автоматически выбирает стратегию маппинга.
 */
public class CsvFileImporter<T> extends AbstractFileImporter<T> {

    public CsvFileImporter(Class<T> importDTOClass, ImportMapper<T> importMapper, Map<String, Object> customFilters) {
        super(importDTOClass, importMapper, customFilters);
    }

    @Override
    protected Iterable<T> iteratorBuilder(InputStream inputStream) {
        // 1. Определяем кодировку и создаем Reader
        InputStreamReader streamReader = EncodingDetector.getReader(inputStream);
        
        // 2. Создаем CSVReader явно, чтобы прочитать заголовок
        try {
            com.opencsv.CSVParser parser = new com.opencsv.CSVParserBuilder()
                    .withSeparator(Constant.SEPARATOR)
                    .build();
            com.opencsv.CSVReader csvReader = new com.opencsv.CSVReaderBuilder(streamReader)
                    .withCSVParser(parser)
                    .build();

            // 3. Читаем заголовок (первую строку)
            String[] header = csvReader.readNext();
            if (header == null) {
                // Пустой файл
                return java.util.Collections.emptyList();
            }

            // 4. Выбираем стратегию
            MappingStrategy<T> strategy = resolveStrategy(importDTOClass);

            // 5. Обработка динамических колонок
            if (strategy instanceof DynamicColumnMappingStrategy) {
                processDynamicColumns(header, importDTOClass);
            }

            // 6. Строим CsvToBean на основе уже открытого csvReader (он уже на 2-й строке)
            // skipLines не нужен, так как мы уже прочитали заголовок
            return new CsvToBeanBuilder<T>(csvReader)
                    .withType(importDTOClass)
                    .withMappingStrategy(strategy)
                    .withIgnoreLeadingWhiteSpace(true)
                    .withFieldAsNull(CSVReaderNullFieldIndicator.BOTH)
                    .withThrowExceptions(true)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Error initializing CSV reader", e);
        }
    }

    private void processDynamicColumns(String[] header, Class<T> dtoClass) {
        DynamicColumn annotation = dtoClass.getAnnotation(DynamicColumn.class);
        int start = annotation.startsFromIndex();
        
        List<String> dynamicHeaders = new java.util.ArrayList<>();
        List<String> allHeaders = new java.util.ArrayList<>();
        
        for (int i = 0; i < header.length; i++) {
            // Очищаем имя колонки от лишних кавычек и пробелов, если нужно, 
            // но OpenCSV обычно отдает "сырые" данные ячейки (без кавычек-оберток).
            // В старом коде могли быть нюансы, здесь берем как есть.
            String colName = header[i] != null ? header[i].trim() : "";
            if (i >= start) {
                dynamicHeaders.add(colName);
            }
            allHeaders.add(colName);
        }

        // Сохраняем в фильтры (для совместимости с мапперами)
        customFilters.put("dynamicHeaders", dynamicHeaders); 
        // В старых мапперах часто используется dynamicColumnHeaderList
        customFilters.put("dynamicColumnHeaderList", dynamicHeaders);
        customFilters.put("allColumnHeaderList", allHeaders);

        // Имя временной таблицы должно быть в фильтрах, или генерируем дефолтное
        String tempTableName = (String) customFilters.get("tempTableName");
        if (tempTableName == null) {
            tempTableName = "temp_upload_" + System.currentTimeMillis();
            customFilters.put("tempTableName", tempTableName);
        }

        // Вызываем создание таблицы
        importMapper.createTempTable(dynamicHeaders, tempTableName);
    }

    /**
     * Определяет стратегию маппинга на основе аннотаций в DTO.
     */
    private MappingStrategy<T> resolveStrategy(Class<T> clazz) {
        // 1. Проверяем наличие @DynamicColumn
        if (clazz.isAnnotationPresent(DynamicColumn.class)) {
            DynamicColumnMappingStrategy<T> strategy = new DynamicColumnMappingStrategy<>();
            strategy.setType(clazz);
            return strategy;
        }

        boolean hasPositionAnnotations = false;

        // Рекурсивно (или просто для текущего класса) проверяем поля
        // OpenCSV смотрит на все поля, включая приватные
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(CsvBindByPosition.class)) {
                hasPositionAnnotations = true;
                break;
            }
        }

        // Приоритеты:
        // 1. Если есть явные позиционные аннотации -> ColumnPositionMappingStrategy
        // 2. Иначе (есть именные или нет никаких) -> HeaderColumnNameMappingStrategy

        if (hasPositionAnnotations) {
            ColumnPositionMappingStrategy<T> strategy = new ColumnPositionMappingStrategy<>();
            strategy.setType(clazz);
            return strategy;
        } else {
            HeaderColumnNameMappingStrategy<T> strategy = new HeaderColumnNameMappingStrategy<>();
            strategy.setType(clazz);
            return strategy;
        }
    }
}