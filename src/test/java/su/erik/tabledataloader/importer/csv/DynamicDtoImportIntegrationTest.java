package su.erik.tabledataloader.importer.csv;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import su.erik.tabledataloader.importer.ImportMapper;
import su.erik.tabledataloader.importer.annotation.DynamicColumn;
import su.erik.tabledataloader.importer.annotation.ImporterBindByIndex;
import su.erik.tabledataloader.importer.loader.CsvFileLoader;
import su.erik.tabledataloader.importer.model.ImportDynamic;
import su.erik.tabledataloader.importer.model.UploadDTO;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DynamicDtoImportIntegrationTest {

    @DynamicColumn(startsFromIndex = 2)
    public static class DynamicImportDTO extends ImportDynamic<Double> {
        private String staticCode;
        private String staticName;

        public DynamicImportDTO(Integer size) { super(size); }

        @ImporterBindByIndex(0)
        public void setStaticCode(String val) { this.staticCode = val; }
        @ImporterBindByIndex(1)
        public void setStaticName(String val) { this.staticName = val; }

        @Override
        public void setterDynamicValue(String value, Integer index) {
            if (value != null && !value.isEmpty()) {
                this.dynamicValues.set(index, Double.valueOf(value.replace(",", ".")));
            }
        }
    }

    static class MockMapper implements ImportMapper<DynamicImportDTO> {
        final List<DynamicImportDTO> items = new ArrayList<>();
        @Override public void insertHeader(UploadDTO uploadDTO) { uploadDTO.setId(1L); }
        @Override public void insert(Map<String, Object> customFilters) { items.add((DynamicImportDTO) customFilters.get("importDTO")); }
        @Override public void createTempTable(List<String> headers, String tableName) {}
        @Override public void delete(long id) {}
        @Override public void finish(Map<String, Object> customFilters) {}
        @Override public void flush() {}
    }

    @Test
    @DisplayName("Integration: DynamicImportDTO structure parsing")
    void testDynamicParsing() {
        String csvData = "StaticCode;StaticName;Region1;Region2\n100;Item1;140.84;250.50";
        ByteArrayInputStream is = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
        MockMapper mapper = new MockMapper();
        CsvFileLoader<DynamicImportDTO> importer = new CsvFileLoader<>(DynamicImportDTO.class, mapper, new HashMap<>());
        importer.importFile(is, "test.csv", csvData.length(), "E", 1L);
        assertEquals(1, mapper.items.size());
        assertEquals(2, mapper.items.get(0).dynamicValues.size());
    }
}
