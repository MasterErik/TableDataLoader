package su.erik.tabledataloader.importer.loader;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.CsvBindByPosition;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import com.opencsv.bean.MappingStrategy;
import su.erik.tabledataloader.config.Constant;
import su.erik.tabledataloader.importer.AbstractFileLoader;
import su.erik.tabledataloader.importer.EncodingDetector;
import su.erik.tabledataloader.importer.ImportMapper;
import su.erik.tabledataloader.importer.annotation.DynamicColumn;
import su.erik.tabledataloader.importer.csv.DynamicColumnMappingStrategy;
import su.erik.tabledataloader.importer.model.ResultDTO;
import su.erik.tabledataloader.importer.model.UploadDTO;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Map;

/**
 * Загрузчик CSV файлов.
 */
public class CsvFileLoader<T> extends AbstractFileLoader<T> {

    public CsvFileLoader(Class<T> importDTOClass, ImportMapper<T> importMapper, Map<String, Object> customFilters) {
        super(importDTOClass, importMapper, customFilters);
    }

    @Override
    protected Iterable<T> iteratorBuilder(InputStream inputStream) { return null; }

    @Override
    public ResultDTO importFile(InputStream inputStream, String name, long size, String entity, Long userId) {
        // Оборачиваем в try-with-resources для автоматического закрытия csvReader и лежащего под ним streamReader
        try (InputStreamReader streamReader = EncodingDetector.getReader(inputStream);
             CSVReader csvReader = new CSVReaderBuilder(streamReader)
                     .withCSVParser(new CSVParserBuilder().withSeparator(Constant.SEPARATOR).build())
                     .build()) {

            String[] currentLine = csvReader.readNext();
            if (currentLine == null) {
                return new ResultDTO(0L, 0);
            }

            UploadDTO uploadDTO = new UploadDTO(name, userId, LocalDate.now(), size, entity);
            importMapper.insertHeader(uploadDTO);
            long uploadId = uploadDTO.getId() != null ? uploadDTO.getId() : 0L;

            MappingStrategy<T> strategy = resolveStrategy(importDTOClass);

            if (isHeader(currentLine)) {
                if (strategy instanceof DynamicColumnMappingStrategy) {
                    ((DynamicColumnMappingStrategy<T>) strategy).setProcessedHeader(currentLine);
                } else if (strategy instanceof HeaderColumnNameMappingStrategy) {
                    // Временный ридер для захвата заголовков из одной строки
                    try (CSVReader headerReader = new CSVReaderBuilder(new java.io.StringReader(String.join(";", currentLine)))
                            .withCSVParser(new CSVParserBuilder().withSeparator(';').build()).build()) {
                        strategy.captureHeader(headerReader);
                    }
                }
                currentLine = csvReader.readNext();
            }

            int importedCount = 0;
            while (currentLine != null) {
                if (currentLine.length > 0 && !(currentLine.length == 1 && currentLine[0].isEmpty())) {
                    T bean = strategy.populateNewBean(currentLine);
                    if (bean != null) {
                        customFilters.put("importDTO", bean);
                        customFilters.put("uploadId", uploadId);
                        importMapper.insert(customFilters);
                        importedCount++;
                    }
                }
                currentLine = csvReader.readNext();
            }

            importMapper.finish(customFilters);
            return new ResultDTO(uploadId, importedCount);

        } catch (Exception exception) {
            throw new com.puls.centralpricing.common.exception.StandardFault(exception);
        }
    }

    private boolean isHeader(String[] line) {
        if (line == null || line.length == 0) return false;
        String firstColumn = line[0];
        if (firstColumn == null) return false;

        firstColumn = firstColumn.replace("\uFEFF", "").trim();
        // Если первая колонка — это число, скорее всего это данные, а не заголовок
        if (firstColumn.matches("^\\d+$")) return false;

        try {
            Long.parseLong(firstColumn);
            return false;
        } catch (NumberFormatException exception) {
            return true;
        }
    }

    private MappingStrategy<T> resolveStrategy(Class<T> dtoClass) {
        if (dtoClass.isAnnotationPresent(DynamicColumn.class)) {
            DynamicColumnMappingStrategy<T> strategy = new DynamicColumnMappingStrategy<>();
            strategy.setType(dtoClass);
            strategy.setImportMapper(importMapper);
            strategy.setCustomFilters(customFilters);
            return strategy;
        }

        boolean hasPositionMapping = false;
        for (Field field : dtoClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(CsvBindByPosition.class)) {
                hasPositionMapping = true;
                break;
            }
        }

        if (hasPositionMapping) {
            ColumnPositionMappingStrategy<T> strategy = new ColumnPositionMappingStrategy<>();
            strategy.setType(dtoClass);
            return strategy;
        } else {
            HeaderColumnNameMappingStrategy<T> strategy = new HeaderColumnNameMappingStrategy<>();
            strategy.setType(dtoClass);
            return strategy;
        }
    }
}