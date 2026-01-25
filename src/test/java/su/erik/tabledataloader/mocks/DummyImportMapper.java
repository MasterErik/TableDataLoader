package su.erik.tabledataloader.mocks;

import su.erik.tabledataloader.importer.ImportMapper;
import su.erik.tabledataloader.importer.model.UploadDTO;

import java.util.List;
import java.util.Map;

public class DummyImportMapper<T> implements ImportMapper<T> {
    @Override
    public void insertHeader(UploadDTO uploadDTO) {
    }

    @Override
    public void insert(Map<String, Object> customFilters) {
    }

    @Override
    public void createTempTable(List<String> headers, String tableName) {
    }

    @Override
    public void delete(long id) {
    }

    @Override
    public void finish(Map<String, Object> customFilters) {
    }

    @Override
    public void flush() {
    }
}