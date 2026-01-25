package su.erik.tabledataloader.importer.csv;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import su.erik.tabledataloader.importer.annotation.DynamicColumn;
import su.erik.tabledataloader.importer.annotation.ImporterBindByIndex;
import su.erik.tabledataloader.importer.loader.CsvFileLoader;
import su.erik.tabledataloader.importer.model.ImportDynamic;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DynamicImportScenarioTest {

    @DynamicColumn(startsFromIndex = 1)
    public static class DynamicImportDTO extends ImportDynamic<Double> {
        private String code;
        public DynamicImportDTO(Integer size) { super(size); }
        @ImporterBindByIndex(0) public void setCode(String value) { this.code = value; }
        @Override public void setterDynamicValue(String value, Integer index) {
            this.dynamicValues.set(index, Double.valueOf(value));
        }
    }

    @Test
    @DisplayName("Stage 1: Проверка DTO")
    void testDtoPopulation() {
        DynamicImportDTO dto = new DynamicImportDTO(3);
        dto.setterDynamicValue("10.5", 0);
        assertEquals(10.5, dto.dynamicValues.get(0));
    }

    @Test
    @DisplayName("Stage 2: Прямой вызов Importer")
    void testDirectImport() {
        String csvData = "Code;Reg1;Reg2\nC1;55.5;66.6";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
        MockMapper<DynamicImportDTO> mapper = new MockMapper<>();
        CsvFileLoader<DynamicImportDTO> importer = new CsvFileLoader<>(DynamicImportDTO.class, mapper, new HashMap<>());

        importer.importFile(inputStream, "test.csv", csvData.length(), "Entity", 1L);

        assertEquals(1, mapper.getImportedItems().size());
    }
}