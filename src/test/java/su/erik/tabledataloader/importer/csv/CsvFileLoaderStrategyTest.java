package su.erik.tabledataloader.importer.csv;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import su.erik.tabledataloader.importer.loader.CsvFileLoader;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvFileLoaderStrategyTest {

    public static class NameDto {
        @CsvBindByName(column = "name") private String name;
        @CsvBindByName(column = "value") private String val;
        public String getName() { return name; }
    }

    public static class PositionDto {
        @CsvBindByPosition(position = 0) private Long id;
        @CsvBindByPosition(position = 1) private String name;
        public Long getId() { return id; }
    }

    @Test
    @DisplayName("Strategy: Should use Name Mapping by default/annotation")
    void testNameStrategy() {
        String csvData = "name;value\nTest;100";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
        MockMapper<NameDto> mapper = new MockMapper<>();
        CsvFileLoader<NameDto> importer = new CsvFileLoader<>(NameDto.class, mapper, new HashMap<>());

        importer.importFile(inputStream, "test.csv", csvData.length(), "Entity", 1L);

        assertEquals(1, mapper.getImportedItems().size());
        assertEquals("Test", mapper.getImportedItems().get(0).getName());
    }

    @Test
    @DisplayName("Strategy: Should use Position Mapping when annotated")
    void testPositionStrategy() {
        String csvData = "id;name\n1;Test\n2;Other";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
        MockMapper<PositionDto> mapper = new MockMapper<>();
        CsvFileLoader<PositionDto> importer = new CsvFileLoader<>(PositionDto.class, mapper, new HashMap<>());

        importer.importFile(inputStream, "test.csv", csvData.length(), "Entity", 1L);

        assertEquals(2, mapper.getImportedItems().size());
    }

    @Test
    @DisplayName("Encoding: Should detect CP1251")
    void testEncodingDetection() {
        String csvData = "name;value\nТест;100";
        byte[] bytes = csvData.getBytes(java.nio.charset.Charset.forName("windows-1251"));
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        MockMapper<NameDto> mapper = new MockMapper<>();
        CsvFileLoader<NameDto> importer = new CsvFileLoader<>(NameDto.class, mapper, new HashMap<>());

        importer.importFile(inputStream, "test.csv", bytes.length, "Entity", 1L);

        assertEquals(1, mapper.getImportedItems().size());
        assertEquals("Тест", mapper.getImportedItems().get(0).getName());
    }
}