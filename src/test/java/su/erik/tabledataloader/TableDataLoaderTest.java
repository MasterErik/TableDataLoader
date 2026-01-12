package su.erik.tabledataloader;

import com.puls.centralpricing.common.exception.InvalidInputParameterException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import su.erik.tabledataloader.config.StandardParam;
import su.erik.tabledataloader.dto.ExportResource;
import su.erik.tabledataloader.dto.InputFile;
import su.erik.tabledataloader.dto.LoaderHttpStatus;
import su.erik.tabledataloader.importer.FileImporter; // Интерфейс
import su.erik.tabledataloader.importer.ImportMapper;
import su.erik.tabledataloader.importer.dto.ImportResultDTO;
import su.erik.tabledataloader.importer.dto.UploadDTO;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class TableDataLoaderTest {

    // --- Stub классы для тестов ---

    // 1. Реализация InputFile
    static class TestInputFile implements InputFile {
        private final String name;
        private final byte[] content;
        public TestInputFile(String name, byte[] content) { this.name = name; this.content = content; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(content); }
        @Override public String getOriginalFilename() { return name; }
        @Override public long getSize() { return content.length; }
    }

    // 2. Реализация ImportMapper (заглушка)
    static class DummyImportMapper implements ImportMapper<Map<String, Object>> {
        @Override public void insertHeader(UploadDTO uploadDTO) {}
        @Override public void createTempTable(List<String> h, String t) {}
        @Override public void insert(Map<String, Object> customFilters) {}
        @Override public void delete(long uploadId) {}
        @Override public void finish(Map<String, Object> customFilters) {}
        @Override public void flush() {}
    }

    // 3. КОНКРЕТНАЯ РЕАЛИЗАЦИЯ FileImporter (Исправление вашей ошибки)
    // Этот класс нужен, чтобы передать его .class в buildArchive
    public static class DummyFileImporter implements FileImporter {
        // Конструктор, который ожидает реальная фабрика (для совместимости сигнатур)
        public DummyFileImporter(Class<?> dtoClass, ImportMapper<?> mapper, Map<String, Object> filters) {}

        @Override
        public ImportResultDTO importFile(InputStream inputStream, String name, long size, String entity, Long userId) {
            return new ImportResultDTO(0, 0);
        }
    }

    // --- Тесты ---

    @Test
    @DisplayName("ARCHIVE: Полный цикл обработки ZIP через ServiceLoader")
    void testBuildArchiveWithServiceLoader() throws IOException {
        // 1. Создаем валидный ZIP архив в памяти
        byte[] zipBytes = createZipBytes("file1.csv", "content1", "file2.xlsx", "content2");
        TestInputFile zipFile = new TestInputFile("data.zip", zipBytes);

        // 2. Подготовка Class объекта с правильными Generics
        // Это решает проблему "incompatible types: Class<Map> cannot be converted to Class<Map<String,Object>>"
        @SuppressWarnings("unchecked")
        Class<Map<String, Object>> dtoClass = (Class<Map<String, Object>>) (Class<?>) Map.class;

        // 3. Настраиваем и запускаем TableDataLoader
        var loader = TableDataLoader.<Map<String, Object>>create()
                .setMapParam(StandardParam.FILE.getKey(), zipFile)
                .setMapParam(StandardParam.ENTITY.getKey(), "TEST_ENTITY")
                .setMapParam(StandardParam.USER_ID.getKey(), 555L)
                .useImportMapper(new DummyImportMapper());
        // 2. Запускаем обработку архива
        var response = loader.buildArchive(
                Map.of(".csv", DummyFileImporter.class, ".xlsx", DummyFileImporter.class),
                dtoClass
        );

        // 4. Проверки
        assertEquals(LoaderHttpStatus.OK, response.getStatus());
        assertNotNull(response.getItems());
        assertEquals(2, response.getTotal());
    }

    @Test
    @DisplayName("EXPORT: Проверка работы через MockFileExporterFactory")
    void testExportWithServiceLoader() {
        ExportResource resource = TableDataLoader.<String>create()
                .useToGetData(p -> List.of("Data1", "Data2"))
                .build(null, "report");

        assertNotNull(resource);
        assertEquals("report.mock", resource.fileName());
        assertEquals(LoaderHttpStatus.OK, resource.status());
    }

    @Test
    @DisplayName("CONTEXT: Проверка загрузки MapParamProvider")
    void testContextLoading() {
        TableDataLoader<String> loader = TableDataLoader.create();
        // MockMapParamProvider (зарегистрированный в test/resources) устанавливает userId = 999
        assertEquals(999L, loader.getMapParam().getUserId());
    }

    @Test
    @DisplayName("VALIDATION: Проверка правила U007 (Pagination requires Sorting)")
    void testCheckValidation() {
        // 1. Пагинация без сортировки -> Ошибка
        var loader1 = TableDataLoader.create()
                .setLimit(10);

        InvalidInputParameterException ex = assertThrows(InvalidInputParameterException.class, () -> loader1.getMapParam().check());
        assertTrue(ex.getMessage().contains("U007"));

        // 2. Пагинация с сортировкой -> OK
        var loader2 = TableDataLoader.create()
                .setLimit(10)
                .addOrderBy("id", "ASC");
        assertDoesNotThrow(() -> loader2.getMapParam().check());

        // 3. Offset без сортировки -> Ошибка
        var loader3 = TableDataLoader.create();
        loader3.getMapParam().setOffset(5);

        InvalidInputParameterException ex3 = assertThrows(InvalidInputParameterException.class, () -> loader3.getMapParam().check());
        assertTrue(ex3.getMessage().contains("U007"));

        // 4. Без пагинации и сортировки -> OK
        var loader4 = TableDataLoader.create();
        assertDoesNotThrow(() -> loader4.getMapParam().check());
    }

    // --- Helper: Создание ZIP ---
    private byte[] createZipBytes(String name1, String content1, String name2, String content2) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry1 = new ZipEntry(name1);
            zos.putNextEntry(entry1);
            zos.write(content1.getBytes());
            zos.closeEntry();

            ZipEntry entry2 = new ZipEntry(name2);
            zos.putNextEntry(entry2);
            zos.write(content2.getBytes());
            zos.closeEntry();
        }
        return baos.toByteArray();
    }
}
