package su.erik.tabledataloader.importer.csv;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import su.erik.tabledataloader.importer.ImportMapper;
import su.erik.tabledataloader.importer.dto.UploadDTO;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvFileImporterStrategyTest {

    // --- DTO for Name Strategy ---
    public static class NameDto {
        @CsvBindByName(column = "ColA")
        private String columnA;
        @CsvBindByName(column = "ColB")
        private String columnB;

        public String getColumnA() { return columnA; }
        public void setColumnA(String columnA) { this.columnA = columnA; }
        public String getColumnB() { return columnB; }
        public void setColumnB(String columnB) { this.columnB = columnB; }
    }

    // --- DTO for Position Strategy ---
    public static class PositionDto {
        @CsvBindByPosition(position = 0)
        private String firstColumn;
        @CsvBindByPosition(position = 1)
        private String secondColumn;

        public String getFirstColumn() { return firstColumn; }
        public void setFirstColumn(String firstColumn) { this.firstColumn = firstColumn; }
        public String getSecondColumn() { return secondColumn; }
        public void setSecondColumn(String secondColumn) { this.secondColumn = secondColumn; }
    }

    // --- Mock Mapper ---
    static class MockMapper<T> implements ImportMapper<T> {
        final List<T> items = new ArrayList<>();
        @Override public void insertHeader(UploadDTO uploadDTO) { uploadDTO.setId(1L); }
        @Override public void insert(Map<String, Object> customFilters) { items.add((T) customFilters.get("importDTO")); }
        @Override public void createTempTable(List<String> headers, String tempTableName) {}
        @Override public void delete(long id) {}
        @Override public void finish(Map<String, Object> customFilters) {}
        @Override public void flush() {}
    }

    public static class RusDto {
        @CsvBindByName(column = "ID") Long id;
        @CsvBindByName(column = "Name") String name;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    @Test
    @DisplayName("Strategy: Should use Name Mapping by default/annotation")
    void testNameStrategy() {
        // Заголовки перепутаны местами. HeaderColumnNameMappingStrategy их прочтет.
        String csv = "ColB;ColA\nvalB;valA";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        MockMapper<NameDto> mapper = new MockMapper<>();
        CsvFileImporter<NameDto> importer = new CsvFileImporter<>(NameDto.class, mapper, new HashMap<>());

        importer.importFile(inputStream, "test.csv", csv.length(), "entity", 1L);

        assertEquals(1, mapper.items.size());
        assertEquals("valA", mapper.items.get(0).getColumnA());
        assertEquals("valB", mapper.items.get(0).getColumnB());
    }

    @Test
    @DisplayName("Strategy: Should use Position Mapping when annotated")
    void testPositionStrategy() {
        // CSV без заголовков
        String csv = "Pos1;Pos2\nNext1;Next2";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        MockMapper<PositionDto> mapper = new MockMapper<>();
        CsvFileImporter<PositionDto> importer = new CsvFileImporter<>(PositionDto.class, mapper, new HashMap<>());

        importer.importFile(inputStream, "test.csv", csv.length(), "entity", 1L);

        assertEquals(2, mapper.items.size());
        assertEquals("Pos1", mapper.items.get(0).getFirstColumn());
        assertEquals("Pos2", mapper.items.get(0).getSecondColumn());
    }

    @Test
    @DisplayName("Encoding: Should detect CP1251")
    void testEncodingDetection() {
        // Создаем строку с русскими символами в CP1251. Нужно побольше текста для guessencoding.
        String text = "ID;Name\n1;Привет мир это тест кодировки для библиотеки чтобы она точно поняла что это русский язык и cp1251 кодировка";
        Charset cp1251 = Charset.forName("windows-1251");
        byte[] bytes = text.getBytes(cp1251);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);

        MockMapper<RusDto> mapper = new MockMapper<>();
        CsvFileImporter<RusDto> importer = new CsvFileImporter<>(RusDto.class, mapper, new HashMap<>());

        importer.importFile(inputStream, "rus.csv", bytes.length, "entity", 1L);

        assertEquals(1, mapper.items.size());
        String importedName = mapper.items.get(0).getName();

        System.out.println("Imported Name: " + importedName);

        assertTrue(importedName.startsWith("Привет"), "Expected name to start with 'Привет', but got: " + importedName);
    }
}
