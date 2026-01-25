package su.erik.tabledataloader.importer.txt;

import org.junit.jupiter.api.Test;
import su.erik.tabledataloader.TableDataLoader;
import su.erik.tabledataloader.dto.DataResponse;
import su.erik.tabledataloader.dto.InputFile;
import su.erik.tabledataloader.importer.ImportMapper;
import su.erik.tabledataloader.importer.loader.FileLoader;
import su.erik.tabledataloader.importer.model.ResultDTO;
import su.erik.tabledataloader.importer.model.UploadDTO;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TxtImportSpiTest {

    public static class TxtDto {
        private String content;
        public String getContent() { return content; }
        public void setContent(String s) { this.content = s; }
    }

    public static class MockTxtMapper implements ImportMapper<TxtDto> {
        final List<TxtDto> items = new ArrayList<>();
        @Override public void insertHeader(UploadDTO uploadDTO) { uploadDTO.setId(10L); }
        @Override public void insert(Map<String, Object> customFilters) { items.add(new TxtDto()); }
        @Override public void createTempTable(List<String> headers, String tableName) {}
        @Override public void delete(long id) {}
        @Override public void finish(Map<String, Object> customFilters) {}
        @Override public void flush() {}
    }

    public static class TxtFileLoader implements FileLoader {
        public TxtFileLoader(Class<?> cls, ImportMapper<?> mapper, Map<String, Object> filters) {}
        @Override
        public ResultDTO importFile(InputStream is, String name, long size, String entity, Long userId) {
            return new ResultDTO(10L, 1);
        }
    }

    @Test
    void testSpiLoading() {
        TableDataLoader.registerLoader("txt", TxtFileLoader.class);

        InputFile file = new InputFile() {
            @Override public InputStream getInputStream() { return new ByteArrayInputStream(new byte[0]); }
            @Override public String getOriginalFilename() { return "test.txt"; }
            @Override public long getSize() { return 0; }
        };

        var loader = TableDataLoader.<TxtDto>create()
                .setMapParam(su.erik.tabledataloader.config.Constant.FILE_PARAM, file)
                .useImportMapper(new MockTxtMapper());

        DataResponse<ResultDTO> response = loader.build(TxtDto.class);
        assertEquals(1, response.items().size());
    }
}