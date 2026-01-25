package su.erik.tabledataloader.mocks;

import su.erik.tabledataloader.importer.ImportMapper;
import su.erik.tabledataloader.importer.loader.FileLoader;
import su.erik.tabledataloader.importer.model.ResultDTO;

import java.io.InputStream;
import java.util.Map;

public class DummyFileLoader implements FileLoader {
    public DummyFileLoader(Class<?> dtoClass, ImportMapper<?> mapper, Map<String, Object> filters) {
    }

    @Override
    public ResultDTO importFile(InputStream inputStream, String fileName, long size, String entity, Long userId) {
        return new ResultDTO(0L, 0L);
    }
}
