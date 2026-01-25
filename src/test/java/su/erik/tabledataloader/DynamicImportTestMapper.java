package su.erik.tabledataloader;

import su.erik.tabledataloader.importer.ImportMapper;
import su.erik.tabledataloader.importer.model.UploadDTO;
import java.util.List;
import java.util.Map;

public interface DynamicImportTestMapper extends ImportMapper<DynamicImportTestDTO> {
    @Override
    void insertHeader(UploadDTO upload);
    @Override
    void insert(Map<String, Object> params);
    @Override
    void createTempTable(List<String> headers, String tableName);
}
