package su.erik.tabledataloader.importer.csv;

import com.opencsv.bean.CsvBindByName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import su.erik.tabledataloader.importer.loader.CsvFileLoader;
import su.erik.tabledataloader.importer.model.ResultDTO;

import java.io.InputStream;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LargeFileImportTest {

    public static class LargeDto {
        @CsvBindByName(column = "id") private Long id;
        @CsvBindByName(column = "val") private String value;
    }

    @Test
    @DisplayName("Streaming: Import 100,000 rows")
    void testLargeImport() {
        InputStream inputStream = new InputStream() {
            private int row = 0;
            private byte[] currentLine = "id;val\n".getBytes();
            private int position = 0;
            @Override public int read() {
                if (position < currentLine.length) return currentLine[position++];
                if (row >= 100000) return -1;
                row++; position = 0;
                currentLine = (row + ";value" + row + "\n").getBytes();
                return currentLine[position++];
            }
        };
        MockMapper<LargeDto> mapper = new MockMapper<>();
        CsvFileLoader<LargeDto> importer = new CsvFileLoader<>(LargeDto.class, mapper, new HashMap<>());
        ResultDTO result = importer.importFile(inputStream, "large.csv", 0, "Entity", 1L);
        assertEquals(100000, result.count());
    }
}