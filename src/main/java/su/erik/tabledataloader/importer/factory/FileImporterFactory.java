package su.erik.tabledataloader.importer.factory;

import su.erik.tabledataloader.importer.ImportMapper;
import su.erik.tabledataloader.importer.loader.FileLoader;

import java.util.Map;

public interface FileImporterFactory {
    <T> FileLoader createImporter(Class<? extends FileLoader> loaderClass, Class<T> dtoClass, ImportMapper<T> mapper, Map<String, Object> filters);
}