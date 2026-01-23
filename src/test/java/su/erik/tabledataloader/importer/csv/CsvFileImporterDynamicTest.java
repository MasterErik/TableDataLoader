package su.erik.tabledataloader.importer.csv;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import su.erik.tabledataloader.importer.ImportMapper;
import su.erik.tabledataloader.importer.annotation.DynamicColumn;
import su.erik.tabledataloader.importer.annotation.ImporterBindByIndex;
import su.erik.tabledataloader.importer.dto.dynamic.CsvImportDynamicDTO;
import su.erik.tabledataloader.importer.dto.UploadDTO;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CsvFileImporterDynamicTest {

    // --- Dynamic DTO ---
    @DynamicColumn(startsFromIndex = 2)
    public static class DynamicDto extends CsvImportDynamicDTO<Double> {
        
        private Long code;
        private String name;

        // Конструктор обязателен!
        public DynamicDto(Integer size) {
            super(size);
        }

        @ImporterBindByIndex(0)
        public void setCode(String val) { this.code = Long.valueOf(val); }

        @ImporterBindByIndex(1)
        public void setName(String val) { this.name = val; }

        @Override
        public void setterDynamicValue(String value, Integer index) {
            this.dynamicValues.set(index, Double.valueOf(value));
        }
        
        // Getters for test
        public Long getCode() { return code; }
        public String getName() { return name; }
    }

    // --- Mock Mapper ---
    static class MockMapper<T> implements ImportMapper<T> {
        final List<T> importedItems = new ArrayList<>();
        @Override public void insertHeader(UploadDTO uploadDTO) { uploadDTO.setId(1L); }
        @Override public void insert(Map<String, Object> customFilters) { importedItems.add((T) customFilters.get("importDTO")); }
        @Override public void createTempTable(List<String> headers, String tempTableName) {}
        @Override public void delete(long uploadId) {}
        @Override public void finish(Map<String, Object> customFilters) {}
        @Override public void flush() {}
    }

    @Test
    @DisplayName("Dynamic: Should map static fields by index and dynamic tail to list")
    void testDynamicColumns() {
        // CSV: Code;Name;Month1;Month2;Month3
        // 101;ProductA;10.5;20.0;30.1
        String csv = "101;ProductA;10.5;20.0;30.1";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        MockMapper<DynamicDto> mapper = new MockMapper<>();
        CsvFileImporter<DynamicDto> importer = new CsvFileImporter<>(DynamicDto.class, mapper, new HashMap<>());

        importer.importFile(inputStream, "dyn.csv", csv.length(), "entity", 1L);

        assertEquals(1, mapper.importedItems.size());
        DynamicDto item = mapper.importedItems.get(0);
        
        // Check static
        assertEquals(101L, item.getCode());
        assertEquals("ProductA", item.getName());
        
        // Check dynamic
        // startsFromIndex=2 -> 0,1 are static. 2,3,4 are dynamic. Total 5 cols. Dynamic size = 3.
        assertEquals(3, item.dynamicValues.size());
        assertEquals(10.5, item.dynamicValues.get(0));
        assertEquals(20.0, item.dynamicValues.get(1));
        assertEquals(30.1, item.dynamicValues.get(2));
    }
}
