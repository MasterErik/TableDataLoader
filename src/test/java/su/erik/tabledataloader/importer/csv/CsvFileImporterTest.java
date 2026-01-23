package su.erik.tabledataloader.importer.csv;

import com.opencsv.bean.CsvBindByName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import su.erik.tabledataloader.importer.ImportMapper;
import su.erik.tabledataloader.importer.dto.ImportResultDTO;
import su.erik.tabledataloader.importer.dto.UploadDTO;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CsvFileImporterTest {

    // --- DTO для теста ---
    public static class TestCsvDTO {
        @CsvBindByName(column = "ID")
        private Long id;

        @CsvBindByName(column = "Name")
        private String name;

        // Геттеры/Сеттеры нужны для OpenCSV (обычно)
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    // --- Mock Mapper ---
    static class MockImportMapper implements ImportMapper<TestCsvDTO> {
        final List<TestCsvDTO> importedItems = new ArrayList<>();
        boolean headerInserted = false;
        boolean finished = false;

        @Override
        public void insertHeader(UploadDTO uploadDTO) {
            uploadDTO.setId(100L); // Эмулируем генерацию ID
            headerInserted = true;
        }

        @Override
        public void insert(Map<String, Object> customFilters) {
            TestCsvDTO dto = (TestCsvDTO) customFilters.get("importDTO");
            if (dto != null) {
                importedItems.add(dto);
            }
        }

        @Override public void createTempTable(List<String> headers, String tempTableName) {}
        @Override public void delete(long uploadId) {}
        @Override public void finish(Map<String, Object> customFilters) { finished = true; }
        @Override public void flush() {}
    }

    @Test
    @DisplayName("CSV: Успешный импорт с маппингом по заголовкам")
    void testImportSuccess() {
        // 1. Подготовка данных CSV (Header + 2 строки)
        String csvData = "ID;Name\n1;TestUser\n2;SecondUser";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));

        MockImportMapper mapper = new MockImportMapper();
        Map<String, Object> filters = new HashMap<>();

        // 2. Создание импортера
        CsvFileImporter<TestCsvDTO> importer = new CsvFileImporter<>(TestCsvDTO.class, mapper, filters);

        // 3. Запуск
        ImportResultDTO result = importer.importFile(inputStream, "test.csv", csvData.length(), "test_entity", 1L);

        // 4. Проверки
        assertEquals(2, result.count());
        assertEquals(100L, result.uploadId()); // ID от маппера
        assertTrue(mapper.headerInserted);
        assertTrue(mapper.finished);
        
        // Проверка данных
        assertEquals(2, mapper.importedItems.size());
        assertEquals(1L, mapper.importedItems.get(0).getId());
        assertEquals("TestUser", mapper.importedItems.get(0).getName());
        assertEquals(2L, mapper.importedItems.get(1).getId());
    }
}
