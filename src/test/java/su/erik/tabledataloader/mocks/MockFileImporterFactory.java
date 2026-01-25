package su.erik.tabledataloader.mocks;

import su.erik.tabledataloader.importer.loader.FileLoader;
import su.erik.tabledataloader.importer.ImportMapper;
import su.erik.tabledataloader.importer.factory.FileImporterFactory;
import su.erik.tabledataloader.importer.model.ResultDTO;

import java.util.Map;

public class MockFileImporterFactory implements FileImporterFactory {
    @Override
    public <T> FileLoader createImporter(Class<? extends FileLoader> importerClass,
                                         Class<T> dtoClass,
                                         ImportMapper<T> mapper,
                                         Map<String, Object> filters) {
        return (inputStream, name, size, entity, userId) -> {
            try {
                inputStream.readAllBytes();
            } catch (Exception exception) {
                // ignore
            }
            return new ResultDTO(123L, 10);
        };
    }
}