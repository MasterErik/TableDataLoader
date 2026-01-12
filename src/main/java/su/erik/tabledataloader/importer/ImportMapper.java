package su.erik.tabledataloader.importer;

import su.erik.tabledataloader.importer.dto.UploadDTO;
import java.util.List;
import java.util.Map;

public interface ImportMapper<T> {
    void insertHeader(UploadDTO upload);
    void createTempTable(List<String> allColumnHeaderList, String tempTableName);
    void insert(Map<String, Object> customFilters);
    void delete(long uploadId);
    void finish(Map<String, Object> customFilters);
    void flush();
}
