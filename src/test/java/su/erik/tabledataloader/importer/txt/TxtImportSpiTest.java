package su.erik.tabledataloader.importer.txt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import su.erik.tabledataloader.TableDataLoader;
import su.erik.tabledataloader.config.StandardParam;
import su.erik.tabledataloader.dto.DataResponse;
import su.erik.tabledataloader.dto.InputFile;
import su.erik.tabledataloader.dto.LoaderHttpStatus;
import su.erik.tabledataloader.importer.ImportMapper;
import su.erik.tabledataloader.importer.dto.ImportResultDTO;
import su.erik.tabledataloader.importer.dto.UploadDTO;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TxtImportSpiTest {

    static class MockTxtMapper implements ImportMapper<TxtDto> {
        final List<String> lines = new ArrayList<>();

        @Override public void insertHeader(UploadDTO upload) { upload.setId(10L); }
        @Override public void insert(Map<String, Object> customFilters) {
            TxtDto dto = (TxtDto) customFilters.get("importDTO");
            lines.add(dto.getContent());
        }
        @Override public void createTempTable(List<String> h, String t) {}
        @Override public void delete(long id) {}
        @Override public void finish(Map<String, Object> f) {}
        @Override public void flush() {}
    }

    static class StringFile implements InputFile {
        private final byte[] bytes;
        public StringFile(String s) { bytes = s.getBytes(StandardCharsets.UTF_8); }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(bytes); }
        @Override public String getOriginalFilename() { return "test.txt"; }
        @Override public long getSize() { return bytes.length; }
    }

    @Test
    @DisplayName("SPI: Import new TXT format via TableDataLoader (Explicit Class)")
    void testTxtImportExplicit() {
        runTest(true);
    }

    @Test
    @DisplayName("SPI: Import new TXT format via TableDataLoader (Auto-Discovery)")
    void testTxtImportAuto() {
        runTest(false);
    }

    private void runTest(boolean explicit) {
        String content = "Line 1\nLine 2\nLine 3";
        InputFile file = new StringFile(content);
        MockTxtMapper mapper = new MockTxtMapper();

        TableDataLoader<TxtDto> loader = TableDataLoader.<TxtDto>create()
                .setMapParam(StandardParam.FILE.getKey(), file)
                .setMapParam(StandardParam.ENTITY.getKey(), "TxtEntity")
                .setMapParam(StandardParam.USER_ID.getKey(), 1L)
                .useImportMapper(mapper);

        DataResponse<ImportResultDTO> response;
        if (explicit) {
            response = loader.build(TxtFileImporter.class, TxtDto.class);
        } else {
            response = loader.build(TxtDto.class); // Авто-обнаружение по расширению .txt
        }

        assertEquals(LoaderHttpStatus.OK, response.getStatus());
        assertEquals(1, response.getItems().size());
        assertEquals(3, response.getItems().get(0).count());

        assertEquals(3, mapper.lines.size());
        assertEquals("Line 1", mapper.lines.get(0));
        assertEquals("Line 3", mapper.lines.get(2));
    }
}
