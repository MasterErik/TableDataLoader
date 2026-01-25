package su.erik.tabledataloader.importer.csv;

import su.erik.tabledataloader.importer.ImportMapper;
import su.erik.tabledataloader.importer.model.UploadDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MockMapper<T> implements ImportMapper<T> {

    private final List<T> importedItems = new ArrayList<>();
    private boolean headerInserted = false;
    private boolean finished = false;

    @Override
    public void insertHeader(UploadDTO uploadDTO) {
        uploadDTO.setId(1L);
        this.headerInserted = true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void insert(Map<String, Object> customFilters) {
        if (customFilters.containsKey("importDTO")) {
            importedItems.add((T) customFilters.get("importDTO"));
        }
    }

    @Override
    public void createTempTable(List<String> headers, String tableName) {
        // No implementation needed for mock
    }

    @Override
    public void delete(long uploadId) {
        // No implementation needed for mock
    }

    @Override
    public void finish(Map<String, Object> customFilters) {
        this.finished = true;
    }

    @Override
    public void flush() {
        // No implementation needed for mock
    }

    public List<T> getImportedItems() {
        return importedItems;
    }

    public boolean isHeaderInserted() {
        return headerInserted;
    }

    public boolean isFinished() {
        return finished;
    }
}
