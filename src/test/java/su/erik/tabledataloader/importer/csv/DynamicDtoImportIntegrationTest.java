package su.erik.tabledataloader.importer.csv;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import su.erik.tabledataloader.config.Constant;
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

class DynamicDtoImportIntegrationTest {

    /**
     * DTO адаптированный из marketing/.../BalanceAssortmentImportDTO.kt
     */
    @DynamicColumn(startsFromIndex = 2)
    public static class BalanceAssortmentImportDTO extends CsvImportDynamicDTO<Double> {

        private Long goodCode;
        private String goodName;

        public BalanceAssortmentImportDTO(Integer dynamicValueSize) {
            super(dynamicValueSize);
        }

        @ImporterBindByIndex(0)
        public void setGoodCode(String value) {
            // Эмулируем логику value.toDouble().toLong() из Kotlin DTO
            try {
                this.goodCode = Double.valueOf(value).longValue();
            } catch (NumberFormatException e) {
                this.goodCode = null;
            }
        }

        @ImporterBindByIndex(1)
        public void setGoodName(String value) {
            this.goodName = value;
        }

        @Override
        public void setterDynamicValue(String value, Integer index) {
            // Логика парсинга Double из Kotlin DTO
            if (value != null && !value.isBlank()) {
                this.dynamicValues.set(index, Double.valueOf(value));
            }
        }

        public Long getGoodCode() { return goodCode; }
        public String getGoodName() { return goodName; }
    }

    static class MockMapper implements ImportMapper<BalanceAssortmentImportDTO> {
        final List<BalanceAssortmentImportDTO> capturedItems = new ArrayList<>();

        @Override
        public void insertHeader(UploadDTO uploadDTO) {
            uploadDTO.setId(123L);
        }

        @Override
        public void insert(Map<String, Object> customFilters) {
            capturedItems.add((BalanceAssortmentImportDTO) customFilters.get("importDTO"));
        }

        @Override
        public void createTempTable(List<String> headers, String tempTableName) {}

        @Override
        public void delete(long uploadId) {}

        @Override
        public void finish(Map<String, Object> customFilters) {}

        @Override
        public void flush() {}
    }

    @Test
    @DisplayName("Integration: BalanceAssortmentImportDTO structure parsing")
    void testBalanceAssortmentParsing() {
        // Эмуляция CSV файла: Header + Data
        // Структура: GoodCode; GoodName; Dynamic1; Dynamic2
        String csvData = "GoodCode;GoodName;Region1;Region2\n" +
                         "3923;Флуимуцил Антибиотик;140.84;250.50";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));

        MockMapper mapper = new MockMapper();
        Map<String, Object> filters = new HashMap<>();
        // Так как файл содержит заголовок, а мы используем позиционный/динамический маппинг,
        // нам нужно явно пропустить первую строку.
        filters.put(Constant.SKIP_LINES, 1);

        CsvFileImporter<BalanceAssortmentImportDTO> importer =
                new CsvFileImporter<>(BalanceAssortmentImportDTO.class, mapper, filters);

        importer.importFile(inputStream, "balance.csv", csvData.length(), "BalanceAssortment", 1L);

        assertEquals(1, mapper.capturedItems.size());
        BalanceAssortmentImportDTO dto = mapper.capturedItems.get(0);

        // Проверка статических полей
        assertEquals(3923L, dto.getGoodCode());
        assertEquals("Флуимуцил Антибиотик", dto.getGoodName());

        // Проверка динамических полей (хвост)
        assertEquals(2, dto.dynamicValues.size());
        assertEquals(140.84, dto.dynamicValues.get(0)); // Region1
        assertEquals(250.50, dto.dynamicValues.get(1)); // Region2
    }
}