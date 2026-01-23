package su.erik.tabledataloader;

import com.puls.centralpricing.common.exception.BaseFaultException;
import com.puls.centralpricing.common.exception.Error;
import su.erik.tabledataloader.archive.ArchiveIterator;
import su.erik.tabledataloader.config.Constant;
import su.erik.tabledataloader.config.StandardParam;
import su.erik.tabledataloader.dto.*;
import su.erik.tabledataloader.importer.dto.ImportResultDTO;
import su.erik.tabledataloader.param.HeaderUtils;
import su.erik.tabledataloader.param.MapParam;
import su.erik.tabledataloader.spi.MapParamProvider;

// Исключения (используем те, что вы предоставили)
import com.puls.centralpricing.common.exception.TableDataLoaderProviderNotFoundException;
import com.puls.centralpricing.common.exception.StandardFault;

// Импорт (новые интерфейсы)
import su.erik.tabledataloader.importer.FileImporter;
import su.erik.tabledataloader.importer.ImportMapper;
import su.erik.tabledataloader.importer.factory.FileImporterFactory;

// Экспорт (новые интерфейсы)
import su.erik.tabledataloader.exporter.FileExporter;
import su.erik.tabledataloader.exporter.ExportedFile;
import su.erik.tabledataloader.exporter.factory.FileExporterFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Универсальный загрузчик данных (TableDataLoader).
 *
 * <p>Обеспечивает единую точку входа для:
 * <ul>
 * <li>Получения списков данных (GET) с пагинацией и фильтрацией.</li>
 * <li>Создания/Обновления сущностей (POST/PUT).</li>
 * <li>Удаления данных (DELETE).</li>
 * <li>Импорта из файлов и архивов.</li>
 * <li>Экспорта в отчеты (Excel, CSV).</li>
 * </ul>
 *
 * <p>Класс полностью изолирован от Spring Framework. Взаимодействие с HTTP/Web слоем
 * должно происходить через адаптеры.
 */
@SuppressWarnings({"unused", "UnusedReturnValue", "unchecked"})
public class TableDataLoader<T> {

    // --- ПОЛЯ И КОНТЕКСТ ---
    private MapParam mapParam;
    private LoaderHttpStatus responseStatus = LoaderHttpStatus.OK;

    // --- СТРАТЕГИИ (Functional Interfaces) ---
    private Function<MapParam, Iterable<T>> getData;      // Чтение списков
    private Function<MapParam, T> saveData;               // Сохранение (Create/Update)
    private Function<MapParam, Long> execData;            // Выполнение (Delete/Procedure)
    private Function<MapParam, Long> countFetcher;        // Подсчет количества (для пагинации)
    private Function<MapParam, Iterable<T>> childListFetcher; // Для Master-Detail

    // --- НАСТРОЙКИ ---
    private List<String> replaceParentByChild;            // Flattening для Master-Detail
    private Consumer<T> forEachConsumer;                  // Пост-обработка записей
    private ImportMapper<T> importMapper;                 // Маппер для импорта

    // --- SERVICE LOADERS (Singleton Providers) ---
    private static final MapParamProvider MAP_PARAM_PROVIDER;
    private static final FileImporterFactory IMPORTER_FACTORY;
    private static final FileExporterFactory EXPORTER_FACTORY;
    private static final Map<String, Class<? extends FileImporter>> IMPORTER_REGISTRY = new HashMap<>();
//    private static final List<ArchiveIteratorFactory> ARCHIVE_FACTORIES;

    static {
        // Загрузка реализаций через SPI при инициализации класса
        MAP_PARAM_PROVIDER = loadService(MapParamProvider.class);
        IMPORTER_FACTORY = loadService(FileImporterFactory.class);
        EXPORTER_FACTORY = loadService(FileExporterFactory.class);

        // Загрузка дескрипторов импортеров
        ServiceLoader<su.erik.tabledataloader.spi.FileImporterDescriptor> descriptors = 
                ServiceLoader.load(su.erik.tabledataloader.spi.FileImporterDescriptor.class);
        for (su.erik.tabledataloader.spi.FileImporterDescriptor descriptor : descriptors) {
            for (String ext : descriptor.getSupportedExtensions()) {
                IMPORTER_REGISTRY.put(ext.toLowerCase(), descriptor.getImporterClass());
            }
        }
    }

    private static <S> S loadService(Class<S> serviceClass) {
        return ServiceLoader.load(serviceClass)
                .findFirst()
                .orElseThrow(() -> new TableDataLoaderProviderNotFoundException(serviceClass.getName() + " provider not found"));
    }

    // Приватный конструктор
    private TableDataLoader() {
        this.mapParam = new MapParam();
        MAP_PARAM_PROVIDER.fill(this.mapParam); // Заполнение контекста (userId, roles...)
    }

    /**
     * Создает новый экземпляр загрузчика.
     */
    public static <T> TableDataLoader<T> create() {
        return new TableDataLoader<>();
    }

    // =================================================================================================================
    //                                         НАСТРОЙКА (FLUENT API)
    // =================================================================================================================

    public TableDataLoader<T> setStatus(LoaderHttpStatus status) {
        this.responseStatus = status;
        return this;
    }

    // --- Стратегии ---

    public TableDataLoader<T> useToGetData(Function<MapParam, Iterable<T>> getData) {
        this.getData = getData;
        return this;
    }

    public TableDataLoader<T> useToSave(Function<MapParam, T> saveData) {
        this.saveData = saveData;
        return this;
    }

    public TableDataLoader<T> useImportMapper(Function<MapParam, Long> execData) {
        this.execData = execData;
        return this;
    }

    public TableDataLoader<T> useToCount(Function<MapParam, Long> countFetcher) {
        this.countFetcher = countFetcher;
        return this;
    }

    public TableDataLoader<T> useChildList(Function<MapParam, Iterable<T>> childListFetcher) {
        this.childListFetcher = childListFetcher;
        return this;
    }

    // --- Дополнительные настройки ---

    public TableDataLoader<T> replaceParentByChild(List<String> fields) {
        this.replaceParentByChild = fields;
        return this;
    }

    public TableDataLoader<T> setForEachConsumer(Consumer<T> consumer) {
        this.forEachConsumer = consumer;
        return this;
    }

    public TableDataLoader<T> useImportMapper(ImportMapper<T> importMapper) {
        this.importMapper = importMapper;
        return this;
    }

    public TableDataLoader<T> setHeaderRowNumber(int headerRowNumber) {
        getMapParam().filter(StandardParam.HEADER_ROW_NUMBER.getKey(), headerRowNumber);
        return this;
    }

    public TableDataLoader<T> setColumnMapper(Map<String, Integer> columnMapper) {
        getMapParam().filter(StandardParam.COLUMN_MAPPER.getKey(), columnMapper);
        return this;
    }

    // --- Параметры (MapParam) ---

    public TableDataLoader<T> setMapParam(MapParam mapParam) {
        this.mapParam = mapParam;
        return this;
    }

    public MapParam getMapParam() {
        if (mapParam == null) mapParam = new MapParam();
        return mapParam;
    }

    public TableDataLoader<T> setMapParam(String field, Object value) {
        getMapParam().filter(field, value);
        return this;
    }

    public TableDataLoader<T> setMapParam(String field, String op, Object value) {
        getMapParam().addCriteria(field, op, value);
        return this;
    }

    public TableDataLoader<T> setLimit(int limit) {
        getMapParam().filter(StandardParam.PER_PAGE.getKey(), limit);
        getMapParam().setLimit(limit); // И в само свойство для синхронизации
        return this;
    }

    public TableDataLoader<T> addOrderBy(String sortBy, String sortOrder) {
        getMapParam().addOrderBy(sortBy, sortOrder);
        return this;
    }

    // =================================================================================================================
    //                                         МЕТОДЫ СБОРКИ (BUILD)
    // =================================================================================================================

    /**
     * Режим сборки для POST/PUT запросов.
     */
    public enum BuildMode {
        SINGLE_ENTITY
    }

    /**
     * BUILD: Чтение списка данных (GET).
     * <p>Поддерживает Master-Detail (если задан childListFetcher) и пагинацию.
     *
     * @return DataResponse со списком и общим количеством записей.
     */
    public DataResponse<T> build() {
        Map<String, String> headers;
        if (getData == null) {
            headers = HeaderUtils.createResponseHeaders(mapParam, 0);
            return new DataResponse<>(Collections.emptyList(), 0L, headers, responseStatus);
        }

        Iterable<T> dataIterable = getData.apply(mapParam);
        List<T> data = new ArrayList<>();
        if (dataIterable != null) {
            dataIterable.forEach(data::add);
        }

        if (forEachConsumer != null) {
            data.forEach(forEachConsumer);
        }

        // Master-Detail обработка
        // Используем getFirst() (Java 21), проверяем, что данные являются Map
        if (childListFetcher != null && !data.isEmpty() && data.getFirst() instanceof Map) {
            processMasterDetail(data);
        }

        Long count = 0L;
        if (countFetcher != null) {
            count = countFetcher.apply(mapParam);
        } else if (!data.isEmpty()) {
            count = (long) data.size();
        }

        // Генерируем заголовки (X-Total-Entries, Pagination)
        headers = HeaderUtils.createResponseHeaders(mapParam, count);

        return new DataResponse<>(data, count, headers, responseStatus);
    }

    /**
     * BUILD: Операции с одной сущностью (POST/PUT).
     *
     * @param mode Режим сборки (SINGLE_ENTITY).
     * @return DataResponse с одной записью в списке.
     */
    public DataResponse<T> build(BuildMode mode) {
        if (mode == BuildMode.SINGLE_ENTITY) {
            if (saveData != null) {
                T result = saveData.apply(mapParam);
                Map<String, String> headers = HeaderUtils.createResponseHeaders(mapParam, 1L);
                return new DataResponse<>(Collections.singletonList(result), 1L, headers, responseStatus);
            }
            return new DataResponse<>(Collections.emptyList(), 0L, Collections.emptyMap(), LoaderHttpStatus.NOT_FOUND);
        }
        throw new IllegalArgumentException("Unknown BuildMode: " + mode);
    }

    /**
     * BUILD: Удаление или выполнение процедуры (DELETE).
     *
     * @return DataResponse с количеством затронутых строк.
     */
    public DataResponse<Long> buildDelete() {
        long count = 0L;
        if (execData != null) {
            count = execData.apply(mapParam);
        }
        Map<String, String> headers = HeaderUtils.createResponseHeaders(mapParam, count);
        return new DataResponse<>(Collections.singletonList(count), count, headers, responseStatus);
    }

    /**
     * BUILD: Экспорт данных в файл.
     * <p>Использует реализацию FileExporterFactory для создания отчета.
     *
     * @param viewClass Класс экспортера (маркерный интерфейс или DTO).
     * @param fileName  Желаемое имя файла.
     * @return ExportResource с потоком данных файла.
     */
    public ExportResource build(Class<? extends FileExporter> viewClass, String fileName) {
        try {
            Iterable<T> result = getData.apply(mapParam);

            // Если данных нет, возвращаем пустой контент (или можно бросить исключение/вернуть 204)
            if (result == null || !result.iterator().hasNext()) {
                return new ExportResource(fileName, Constant.CONTENT_TYPE_OCTET_STREAM, InputStream.nullInputStream(), 0, LoaderHttpStatus.NO_CONTENT);
            }

            FileExporter exporter;
            // Поддержка Consumer'а при экспорте (если нужно потоковое обогащение)
            if (forEachConsumer != null) {
                exporter = EXPORTER_FACTORY.createExporter(result, viewClass, forEachConsumer);
            } else {
                exporter = EXPORTER_FACTORY.createExporter(result, viewClass);
            }

            // Выполняем экспорт. Метод export() возвращает абстракцию ExportedFile (не Spring Resource!)
            ExportedFile exportedFile = exporter.export();
            String fullFileName = exporter.getFullFileName(fileName);

            return new ExportResource(
                    fullFileName,
                    Constant.CONTENT_TYPE_OCTET_STREAM,
                    exportedFile.getInputStream(),
                    exportedFile.contentLength(),
                    responseStatus
            );
        } catch (Exception e) {
            throw new StandardFault(e);
        }
    }

    /**
     * BUILD: Импорт одного файла.
     * <p>Требует наличия ImportMapper и параметра InputFile в MapParam.
     *
     * @param importerClass Класс конкретного импортера (реализация FileImporter).
     * @param dtoClass      Класс DTO, в который парсится файл.
     * @return DataResponse со статистикой импорта.
     */
    public DataResponse<ImportResultDTO> build(Class<? extends FileImporter> importerClass, Class<T> dtoClass) {
        if (importMapper == null) throw new IllegalStateException("ImportMapper not set");

        try {
            InputFile file = getInputFile();
            String entity = (String) mapParam.getFilters().get(Constant.ENTITY_PARAM);
            Long userId = mapParam.getUserId();

            // Создаем импортер через фабрику
            FileImporter fileImporter = IMPORTER_FACTORY.createImporter(importerClass, dtoClass, importMapper, mapParam.getFilters());

            try (InputStream is = file.getInputStream()) {
                ImportResultDTO result = fileImporter.importFile(is, file.getOriginalFilename(), file.getSize(), entity, userId);
                return createImportResponse(Collections.singletonList(result));
            }
        } catch (Exception e) {
            throw new StandardFault(e);
        }
    }

    /**
     * BUILD: Импорт одного файла с автоматическим выбором импортера по расширению.
     *
     * @param dtoClass Класс DTO.
     * @return DataResponse со статистикой импорта.
     */
    public DataResponse<ImportResultDTO> build(Class<T> dtoClass) {
        InputFile file = getInputFile();
        String filename = file.getOriginalFilename();
        String ext = getExtension(filename).replace(".", "").toLowerCase();

        Class<? extends FileImporter> importerClass = IMPORTER_REGISTRY.get(ext);
        if (importerClass == null) {
            throw new BaseFaultException(Error.U014, "No importer found for extension: " + ext);
        }

        return build(importerClass, dtoClass);
    }

    /**
     * BUILD: Импорт архива (ZIP и др.).
     * <p>Итерируется по файлам архива и применяет соответствующие импортеры.
     *
     * @param importersMap Карта соответствия расширений файлов и классов импортеров.
     * @param dtoClass     Класс DTO.
     * @return DataResponse со статистикой по каждому файлу.
     */
    public DataResponse<ImportResultDTO> buildArchive(
            Map<String, Class<? extends FileImporter>> importersMap,
            Class<T> dtoClass) {

        if (importMapper == null) throw new IllegalStateException("ImportMapper not set");

        try {
            InputFile archiveFile = getInputFile();
            String entity = (String) mapParam.getFilters().get(Constant.ENTITY_PARAM);
            Long userId = mapParam.getUserId();

            List<ImportResultDTO> results = new ArrayList<>();

            // TODO: Здесь будет вызов ArchiveIteratorFactory.createIterator(...)
            try (InputStream is = archiveFile.getInputStream();
                 ArchiveIterator iterator = new ArchiveIterator(is, archiveFile.getOriginalFilename())) {

                while (iterator.hasNext()) {
                    var entry = iterator.next();
                    String fileName = entry.name();
                    // Выбор импортера по расширению
                    String ext = getExtension(fileName);
                    Class<? extends FileImporter> importerClass = importersMap.get(ext);

                    if (importerClass == null) continue; // Пропуск неподдерживаемых файлов

                    FileImporter fileImporter = IMPORTER_FACTORY.createImporter(importerClass, dtoClass, importMapper, mapParam.getFilters());

                    // Импортируем поток записи. Не закрываем его внутри импортера!
                    ImportResultDTO res = fileImporter.importFile(entry.content(), fileName, entry.size(), entity, userId);
                    results.add(res);
                }
                return createImportResponse(results);
            }
        } catch (Exception e) {
            throw new StandardFault(e);
        }
    }

    // =================================================================================================================
    //                                         ВНУТРЕННИЕ МЕТОДЫ (HELPERS)
    // =================================================================================================================

    private InputFile getInputFile() {
        Object fileObj = mapParam.getFilters().get(Constant.FILE_PARAM);
        if (!(fileObj instanceof InputFile)) {
            throw new BaseFaultException(Error.U014);
        }
        return (InputFile) fileObj;
    }

    private DataResponse<ImportResultDTO> createImportResponse(List<ImportResultDTO> results) {
        long totalFiles = results.size();
        Map<String, String> headers = HeaderUtils.createResponseHeaders(mapParam, totalFiles);
        return new DataResponse<>(results, totalFiles, headers, responseStatus);
    }

    private String getExtension(String fileName) {
        if (fileName == null) return "";
        int i = fileName.lastIndexOf('.');
        return (i > 0) ? fileName.substring(i) : "";
    }

    // --- Логика Master-Detail (Группировка и привязка детей) ---

    private void processMasterDetail(List<T> data) {
        // 1. Собираем ID мастеров
        List<Object> masterIds = data.stream()
                .map(item -> ((Map<String, Object>) item).get(Constant.MASTER_ID))
                .filter(Objects::nonNull)
                .map(this::formatMasterId)
                .collect(Collectors.toList());

        mapParam.setMasterListId(masterIds);

        // 2. Загружаем детей одним запросом
        Iterable<T> childrenIter = childListFetcher.apply(mapParam);
        List<Map<String, Object>> children = new ArrayList<>();
        if (childrenIter != null) {
            childrenIter.forEach(item -> children.add((Map<String, Object>) item));
        }

        if (children.isEmpty()) return;

        // 3. Группируем по ID родителя
        Map<Object, List<Map<String, Object>>> childrenByMasterId = children.stream()
                .filter(m -> m.get(Constant.MASTER_ID) != null)
                .collect(Collectors.groupingBy(m -> m.get(Constant.MASTER_ID)));

        // 4. Распределяем детей по родителям
        for (T item : data) {
            Map<String, Object> master = (Map<String, Object>) item;
            Object masterId = master.get(Constant.MASTER_ID);

            List<Map<String, Object>> relevant = (masterId != null)
                    ? childrenByMasterId.getOrDefault(masterId, Collections.emptyList())
                    : Collections.emptyList();

            assignRecord(master, relevant);
        }
    }

    private void assignRecord(Map<String, Object> master, List<Map<String, Object>> child) {
        // Логика Flattening (замена родителя единственным ребенком)
        if (replaceParentByChild != null) {
            if (child.size() == 1) {
                master.clear();
                master.putAll(child.getFirst()); // Используем getFirst()
                return;
            }
            // Если детей много, удаляем из них лишние поля
            if (!replaceParentByChild.isEmpty()) {
                child.forEach(record -> replaceParentByChild.forEach(record.keySet()::remove));
            }
        }
        // Стандартная вставка списка детей
        master.put(Constant.EXPANDED_KEY, child);
    }

    private Object formatMasterId(Object item) {
        // Форматирование ID для SQL IN (...)
        if (item instanceof String) return "'" + item + "'";
        return item;
    }
}
