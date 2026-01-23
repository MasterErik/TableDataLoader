package su.erik.tabledataloader.importer.csv;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import su.erik.tabledataloader.importer.ImportMapper;
import su.erik.tabledataloader.importer.dto.ImportResultDTO;
import su.erik.tabledataloader.importer.dto.UploadDTO;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LargeFileImportTest {

    // --- 1. Streaming InputStream ---
    /**
     * Генерирует CSV данные на лету.
     * Формат: ID;Name
     * 1;Row_1
     * 2;Row_2
     * ...
     */
    static class CsvGeneratorInputStream extends InputStream {
        private final long totalRows;
        private long currentRow = 0;
        private byte[] buffer;
        private int bufferPos = 0;
        private boolean headerSent = false;

        public CsvGeneratorInputStream(long totalRows) {
            this.totalRows = totalRows;
        }

        @Override
        public int read() throws IOException {
            if (buffer == null || bufferPos >= buffer.length) {
                if (!fillBuffer()) {
                    return -1; // End of stream
                }
            }
            return buffer[bufferPos++] & 0xFF;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (buffer == null || bufferPos >= buffer.length) {
                if (!fillBuffer()) {
                    return -1;
                }
            }
            int available = buffer.length - bufferPos;
            int toCopy = Math.min(len, available);
            System.arraycopy(buffer, bufferPos, b, off, toCopy);
            bufferPos += toCopy;
            return toCopy;
        }

        private boolean fillBuffer() {
            if (!headerSent) {
                buffer = "ID;Name\n".getBytes(StandardCharsets.UTF_8);
                headerSent = true;
                bufferPos = 0;
                return true;
            }

            if (currentRow >= totalRows) {
                return false;
            }

            currentRow++;
            String line = currentRow + ";Row_" + currentRow + "\n";
            buffer = line.getBytes(StandardCharsets.UTF_8);
            bufferPos = 0;
            return true;
        }
    }

    // --- 2. Stateless Mapper ---
    static class StreamingMapper implements ImportMapper<CsvFileImporterStrategyTest.PositionDto> {
        long count = 0;

        @Override
        public void insertHeader(UploadDTO upload) {
            upload.setId(999L);
        }

        @Override
        public void insert(Map<String, Object> customFilters) {
            count++;
            // Не сохраняем объект, чтобы не забить память в тесте!
        }

        @Override public void createTempTable(List<String> h, String t) {}
        @Override public void delete(long id) {}
        @Override public void finish(Map<String, Object> f) {}
        @Override public void flush() {}
    }

    @Test
    @DisplayName("Streaming: Import 100,000 rows with low memory footprint")
    void testLargeImport() {
        long totalRows = 100_000; // Достаточно много, чтобы заметить проблемы, но быстро для unit теста
        InputStream inputStream = new CsvGeneratorInputStream(totalRows);
        
        StreamingMapper mapper = new StreamingMapper();
        
        // Используем PositionDto (позиционный маппинг), чтобы OpenCSV не пытался читать заголовок повторно
        CsvFileImporter<CsvFileImporterStrategyTest.PositionDto> importer = 
                new CsvFileImporter<>(CsvFileImporterStrategyTest.PositionDto.class, mapper, new java.util.HashMap<>());

        long start = System.currentTimeMillis();
        
        // Запускаем импорт
        // Размер файла указываем 0, так как он неизвестен заранее (стрим)
        ImportResultDTO result = importer.importFile(inputStream, "large.csv", 0, "LargeEntity", 1L);

        long duration = System.currentTimeMillis() - start;
        System.out.println("Imported " + totalRows + " rows in " + duration + " ms");

        assertEquals(totalRows, result.count());
        assertEquals(totalRows, mapper.count);
    }
}
