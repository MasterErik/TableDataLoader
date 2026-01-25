package su.erik.tabledataloader.importer.loader;

import su.erik.tabledataloader.importer.model.ResultDTO;

import java.io.InputStream;

public interface FileLoader {
    ResultDTO importFile(InputStream inputStream, String name, long size, String entity, Long userId);
}