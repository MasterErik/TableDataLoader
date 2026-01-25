package su.erik.tabledataloader.importer;

import su.erik.tabledataloader.importer.loader.FileLoader;
import su.erik.tabledataloader.importer.model.ResultDTO;

import java.io.InputStream;
import java.util.Map;

public abstract class AbstractFileLoader<T> implements FileLoader {

    protected final Class<T> importDTOClass;
    protected final ImportMapper<T> importMapper;
    protected final Map<String, Object> customFilters;

    public AbstractFileLoader(Class<T> importDTOClass, ImportMapper<T> importMapper, Map<String, Object> customFilters) {
        this.importDTOClass = importDTOClass;
        this.importMapper = importMapper;
        this.customFilters = customFilters;
    }

    protected abstract Iterable<T> iteratorBuilder(InputStream inputStream);

    @Override
    public ResultDTO importFile(InputStream inputStream, String name, long size, String entity, Long userId) {
        Iterable<T> iterator = iteratorBuilder(inputStream);
        return importRecords(iterator, customFilters);
    }

    protected ResultDTO importRecords(Iterable<T> iterator, Map<String, Object> customFilters) {
        long count = 0;
        for (T importDTO : iterator) {
            if (importDTO == null) continue;
            customFilters.put("importDTO", importDTO);
            importMapper.insert(customFilters);
            count++;
        }
        importMapper.finish(customFilters);
        return new ResultDTO(0L, count);
    }
}
