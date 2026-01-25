package su.erik.tabledataloader.importer.csv;

import com.opencsv.bean.CsvBindByName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import su.erik.tabledataloader.importer.loader.CsvFileLoader;
import su.erik.tabledataloader.importer.model.ResultDTO;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvFileLoaderTest {

    public static class TestCsvDTO {
        @CsvBindByName(column = "ID")
        private Long id;
        @CsvBindByName(column = "Name")
        private String name;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    @Test
    @DisplayName("CSV: Успешный импорт с маппингом по заголовкам")
    void testImportSuccess() {
        String csvData = "ID;Name\n1;TestUser\n2;SecondUser";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));

        MockMapper<TestCsvDTO> mapper = new MockMapper<>();
        Map<String, Object> filters = new HashMap<>();

        CsvFileLoader<TestCsvDTO> importer = new CsvFileLoader<>(TestCsvDTO.class, mapper, filters);
        ResultDTO result = importer.importFile(inputStream, "test.csv", csvData.length(), "Entity", 1L);

        assertEquals(2, result.count());
        assertEquals(1L, result.uploadId());
        assertEquals(2, mapper.getImportedItems().size());
    }
}
