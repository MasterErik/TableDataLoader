package su.erik.tabledataloader.importer;

import su.erik.tabledataloader.importer.model.UploadDTO;

import java.util.List;
import java.util.Map;

public interface ImportMapper<T> {
    void insertHeader(UploadDTO uploadDTO);
    void createTempTable(List<String> headers, String tempTableName);
    void insert(Map<String, Object> customFilters);
    void delete(long uploadId);
    void finish(Map<String, Object> customFilters);
    void flush();
}
