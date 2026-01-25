package su.erik.tabledataloader;

import com.puls.centralpricing.common.exception.BaseFaultException;
import com.puls.centralpricing.common.exception.Error;
import com.puls.centralpricing.common.exception.StandardFault;
import su.erik.tabledataloader.config.Constant;
import su.erik.tabledataloader.config.StandardParam;
import su.erik.tabledataloader.dto.DataResponse;
import su.erik.tabledataloader.dto.ExportResource;
import su.erik.tabledataloader.dto.InputFile;
import su.erik.tabledataloader.dto.LoaderHttpStatus;
import su.erik.tabledataloader.exporter.ExportedFile;
import su.erik.tabledataloader.exporter.FileExporter;
import su.erik.tabledataloader.importer.ImportMapper;
import su.erik.tabledataloader.importer.loader.FileLoader;
import su.erik.tabledataloader.importer.model.ResultDTO;
import su.erik.tabledataloader.exporter.ZipExporter;
import su.erik.tabledataloader.context.DataLoaderContext;
import su.erik.tabledataloader.param.HeaderUtils;
import su.erik.tabledataloader.param.MapParam;
import su.erik.tabledataloader.spi.MapParamProvider;

import java.io.InputStream;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Универсальный загрузчик данных (TableDataLoader).
 */
@SuppressWarnings({"unused", "UnusedReturnValue", "unchecked"})
public class TableDataLoader<T> {

    public enum BuildMode { SINGLE_ENTITY }

    private final DataLoaderContext context;
    private MapParam mapParam;
    private LoaderHttpStatus responseStatus = LoaderHttpStatus.OK;
    private Function<MapParam, Iterable<T>> getDataStrategy;
    private Function<MapParam, T> saveDataStrategy;
    private Function<MapParam, Long> execDataStrategy;
    private Function<MapParam, Long> countFetcherStrategy;
    private Function<MapParam, Iterable<T>> childListFetcherStrategy;
    private List<String> replaceParentByChildFields;
    private Consumer<T> forEachConsumer;
    private ImportMapper<T> importMapper;
    private boolean archiveResult = false;

    private TableDataLoader(DataLoaderContext context) {
        this.context = context;
        this.mapParam = new MapParam();
        if (context.getMapParamProvider() != null) {
            context.getMapParamProvider().fill(this.mapParam);
        }
    }

    public static <T> TableDataLoader<T> create() {
        return new TableDataLoader<>(DataLoaderContext.getDefault());
    }

    public static <T> TableDataLoader<T> create(DataLoaderContext context) {
        return new TableDataLoader<>(context);
    }

    public static void registerLoader(String extension, Class<? extends FileLoader> loaderClass) {
        DataLoaderContext.getDefault().getLoaderRegistry().registerLoader(extension, loaderClass);
    }

    public static void registerExporter(String extension, Class<? extends FileExporter> exporterClass) {
        DataLoaderContext.getDefault().getLoaderRegistry().registerExporter(extension, exporterClass);
    }

    public static synchronized void resetAndInitRegistries() {
        DataLoaderContext.getDefault().getLoaderRegistry().refresh();
    }

    public TableDataLoader<T> setStatus(LoaderHttpStatus status) { this.responseStatus = status; return this; }
    public TableDataLoader<T> useToGetData(Function<MapParam, Iterable<T>> strategy) { this.getDataStrategy = strategy; return this; }
    public TableDataLoader<T> useToSave(Function<MapParam, T> strategy) { this.saveDataStrategy = strategy; return this; }
    public TableDataLoader<T> useToExec(Function<MapParam, Long> strategy) { this.execDataStrategy = strategy; return this; }
    public TableDataLoader<T> useToCount(Function<MapParam, Long> strategy) { this.countFetcherStrategy = strategy; return this; }
    public TableDataLoader<T> useChildList(Function<MapParam, Iterable<T>> strategy) { this.childListFetcherStrategy = strategy; return this; }
    public TableDataLoader<T> replaceParentByChild(List<String> fields) { this.replaceParentByChildFields = fields; return this; }
    public TableDataLoader<T> setForEachConsumer(Consumer<T> consumer) { this.forEachConsumer = consumer; return this; }
    public TableDataLoader<T> useImportMapper(ImportMapper<T> mapper) { this.importMapper = mapper; return this; }
    public TableDataLoader<T> archiveResult() { this.archiveResult = true; return this; }
    public TableDataLoader<T> setMapParam(MapParam param) { this.mapParam = param; return this; }
    public MapParam getMapParam() { if (mapParam == null) mapParam = new MapParam(); return mapParam; }
    
    public TableDataLoader<T> setMapParam(String field, Object value) { getMapParam().filter(field, value); return this; }
    public TableDataLoader<T> setMapParam(String field, String operator, Object value) { getMapParam().addCriteria(field, operator, value); return this; }
    public TableDataLoader<T> setLimit(int limit) { getMapParam().setLimit(limit); return this; }
    public TableDataLoader<T> addOrderBy(String sortField, String sortOrder) { getMapParam().addOrderBy(sortField, sortOrder); return this; }
    public TableDataLoader<T> setHeaderRowNumber(int rowNumber) { getMapParam().filter(StandardParam.HEADER_ROW_NUMBER.getKey(), rowNumber); return this; }
    public TableDataLoader<T> setColumnMapper(Map<String, Integer> columnMapper) { getMapParam().filter(StandardParam.COLUMN_MAPPER.getKey(), columnMapper); return this; }

    public DataResponse<T> build() {
        if (getDataStrategy == null) return new DataResponse<>(Collections.emptyList(), 0L, HeaderUtils.createResponseHeaders(mapParam, 0), responseStatus);
        List<T> items = new ArrayList<>();
        Iterable<T> iterator = getDataStrategy.apply(mapParam);
        if (iterator != null) iterator.forEach(items::add);
        if (forEachConsumer != null) items.forEach(forEachConsumer);
        if (childListFetcherStrategy != null && !items.isEmpty() && items.get(0) instanceof Map) processMasterDetail(items);
        long totalCount = (countFetcherStrategy != null) ? countFetcherStrategy.apply(mapParam) : items.size();
        return new DataResponse<>(items, totalCount, HeaderUtils.createResponseHeaders(mapParam, totalCount), responseStatus);
    }

    public DataResponse<T> build(BuildMode mode) {
        if (mode == BuildMode.SINGLE_ENTITY && saveDataStrategy != null) {
            T result = saveDataStrategy.apply(mapParam);
            return new DataResponse<>(Collections.singletonList(result), 1L, HeaderUtils.createResponseHeaders(mapParam, 1), responseStatus);
        }
        return new DataResponse<>(Collections.emptyList(), 0L, Collections.emptyMap(), LoaderHttpStatus.NOT_FOUND);
    }

    public DataResponse<Long> buildDelete() {
        long deletedCount = (execDataStrategy != null) ? execDataStrategy.apply(mapParam) : 0L;
        return new DataResponse<>(Collections.singletonList(deletedCount), deletedCount, HeaderUtils.createResponseHeaders(mapParam, deletedCount), responseStatus);
    }

    public ExportResource build(String fileName) {
        String extension = getExtension(fileName).replace(".", "").toLowerCase();
        Class<? extends FileExporter> viewClass = context.getLoaderRegistry().getExporterClass(extension);
        if (viewClass == null) throw new BaseFaultException(Error.U014, "No exporter found for extension: " + extension);
        return build(viewClass, fileName);
    }

    public ExportResource build(Class<? extends FileExporter> viewClass, String fileName) {
        try {
            Iterable<T> result = getDataStrategy.apply(mapParam);
            if (result == null || !result.iterator().hasNext()) return new ExportResource(fileName, Constant.CONTENT_TYPE_OCTET_STREAM, InputStream.nullInputStream(), 0, LoaderHttpStatus.NO_CONTENT);

            FileExporter exporter;
            if (forEachConsumer != null) {
                exporter = context.getLoaderRegistry().createExporter(result, viewClass, forEachConsumer);
            } else {
                exporter = context.getLoaderRegistry().createExporter(result, viewClass);
            }

            if (archiveResult) {
                exporter = new ZipExporter(exporter, fileName);
            }

            try (ExportedFile exportedFile = exporter.export()) {
                return new ExportResource(
                        exporter.getFullFileName(fileName),
                        Constant.CONTENT_TYPE_OCTET_STREAM,
                        exportedFile.getInputStream(),
                        exportedFile.contentLength(),
                        responseStatus
                );
            }
        } catch (Exception exception) { throw new StandardFault(exception); }
    }

    public DataResponse<ResultDTO> build(Class<T> dtoClass) {
        InputFile inputFile = getInputFile();
        String extension = getExtension(inputFile.getOriginalFilename()).replace(".", "").toLowerCase();
        return build(extension, dtoClass);
    }

    public DataResponse<ResultDTO> build(String extension, Class<T> dtoClass) {
        if (importMapper == null) throw new IllegalStateException("ImportMapper not set");
        try {
            InputFile inputFile = getInputFile();
            FileLoader loader = context.getLoaderRegistry().createLoader(extension, dtoClass, importMapper, mapParam.getFilters());
            return executeLoad(loader, inputFile);
        } catch (BaseFaultException exception) { throw exception; } catch (Exception exception) { throw new StandardFault(exception); }
    }

    public DataResponse<ResultDTO> build(Class<? extends FileLoader> loaderClass, Class<T> dtoClass) {
        if (importMapper == null) throw new IllegalStateException("ImportMapper not set");
        try {
            InputFile inputFile = getInputFile();
            FileLoader loader = context.getLoaderRegistry().createLoader(loaderClass, dtoClass, importMapper, mapParam.getFilters());
            return executeLoad(loader, inputFile);
        } catch (BaseFaultException exception) { throw exception; } catch (Exception exception) { throw new StandardFault(exception); }
    }

    private DataResponse<ResultDTO> executeLoad(FileLoader loader, InputFile inputFile) {
        try (InputStream inputStream = inputFile.getInputStream()) {
            String fileName = inputFile.getOriginalFilename();
            long fileSize = inputFile.getSize();
            String entity = (String) mapParam.getFilters().get(Constant.ENTITY_PARAM);
            Long userId = mapParam.getUserId();

            ResultDTO result = loader.importFile(inputStream, fileName, fileSize, entity, userId);
            return createImportResponse(Collections.singletonList(result));
        } catch (Exception exception) {
            throw new StandardFault(exception);
        }
    }

    private InputFile getInputFile() {
        Object fileObject = mapParam.getFilters().get(Constant.FILE_PARAM);
        if (!(fileObject instanceof InputFile)) throw new BaseFaultException(Error.U014);
        return (InputFile) fileObject;
    }

    private DataResponse<ResultDTO> createImportResponse(List<ResultDTO> results) {
        long totalCount = results.stream().mapToLong(ResultDTO::count).sum();
        return new DataResponse<>(results, totalCount, HeaderUtils.createResponseHeaders(mapParam, totalCount), responseStatus);
    }

    private String getExtension(String fileName) {
        if (fileName == null) return "";
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex >= 0) ? fileName.substring(dotIndex + 1) : "";
    }

    private void processMasterDetail(List<T> data) {
        List<Object> masterIds = data.stream().map(item -> ((Map<String, Object>) item).get(Constant.MASTER_ID)).filter(Objects::nonNull).map(id -> (id instanceof String) ? "'" + id + "'" : id).collect(Collectors.toList());
        mapParam.setMasterListId(masterIds);
        Iterable<T> childrenIterator = childListFetcherStrategy.apply(mapParam);
        if (childrenIterator == null) return;
        List<Map<String, Object>> children = new ArrayList<>();
        childrenIterator.forEach(item -> children.add((Map<String, Object>) item));
        if (children.isEmpty()) return;
        Map<Object, List<Map<String, Object>>> groupedChildren = children.stream().filter(map -> map.get(Constant.MASTER_ID) != null).collect(Collectors.groupingBy(map -> map.get(Constant.MASTER_ID)));
        for (T item : data) {
            Map<String, Object> master = (Map<String, Object>) item;
            Object masterId = master.get(Constant.MASTER_ID);
            List<Map<String, Object>> relevantChildren = (masterId != null) ? groupedChildren.getOrDefault(masterId, Collections.emptyList()) : Collections.emptyList();
            assignRecord(master, relevantChildren);
        }
    }

    private void assignRecord(Map<String, Object> master, List<Map<String, Object>> children) {
        if (replaceParentByChildFields != null && !replaceParentByChildFields.isEmpty()) {
            if (children.size() == 1) { master.clear(); master.putAll(children.get(0)); return; }
            children.forEach(record -> replaceParentByChildFields.forEach(record.keySet()::remove));
        }
        master.put(Constant.EXPANDED_KEY, children);
    }
}