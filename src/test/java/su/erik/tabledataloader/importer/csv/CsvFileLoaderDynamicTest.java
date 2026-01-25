package su.erik.tabledataloader.importer.csv;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import su.erik.tabledataloader.importer.annotation.DynamicColumn;
import su.erik.tabledataloader.importer.annotation.ImporterBindByIndex;
import su.erik.tabledataloader.importer.loader.CsvFileLoader;
import su.erik.tabledataloader.importer.model.ImportDynamic;
import su.erik.tabledataloader.importer.model.ResultDTO;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvFileLoaderDynamicTest {

    @DynamicColumn(startsFromIndex = 2)
    public static class DynamicDto extends ImportDynamic<Double> {
        private Long code;
        private String name;

        public DynamicDto(Integer size) { super(size); }

        @ImporterBindByIndex(0)
        public void setCode(String value) { this.code = Long.valueOf(value); }

        @ImporterBindByIndex(1)
        public void setName(String value) { this.name = value; }

        @Override
        public void setterDynamicValue(String value, Integer index) {
            this.dynamicValues.set(index, Double.valueOf(value));
        }

        public Long getCode() { return code; }
        public String getName() { return name; }
    }

    @Test
    @DisplayName("Dynamic: Should map static fields by index and dynamic tail to list")
    void testDynamicColumns() {
        String csvData = "Code;Name;Month1;Month2;Month3\n101;ProductA;10.5;20.0;30.1";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));

        MockMapper<DynamicDto> mapper = new MockMapper<>();
        Map<String, Object> filters = new HashMap<>();

        CsvFileLoader<DynamicDto> importer = new CsvFileLoader<>(DynamicDto.class, mapper, filters);
        ResultDTO result = importer.importFile(inputStream, "dyn.csv", csvData.length(), "Entity", 1L);

        assertEquals(1, result.count());
        assertEquals(1, mapper.getImportedItems().size());
        DynamicDto item = mapper.getImportedItems().get(0);

        assertEquals(101L, item.getCode());
        assertEquals(3, item.dynamicValues.size());
        assertEquals(10.5, item.dynamicValues.get(0));
    }
}
