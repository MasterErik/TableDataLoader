package su.erik.tabledataloader.exporter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import su.erik.tabledataloader.TableDataLoader;
import su.erik.tabledataloader.dto.ExportResource;
import su.erik.tabledataloader.exporter.csv.CsvFileExporter;
import su.erik.tabledataloader.context.DataLoaderContext;
import su.erik.tabledataloader.LoaderRegistry;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

class ExportArchivingTest {

    @Test
    @DisplayName("EXPORT: Проверка создания ZIP-архива при экспорте")
    void testExportWithArchiving() throws Exception {
        // Создаем изолированный контекст
        LoaderRegistry registry = new LoaderRegistry();
        // Фабрика больше не нужна, registry сам создает
        registry.registerExporter("csv", CsvFileExporter.class);
        DataLoaderContext context = new DataLoaderContext(registry, null);

        // Подготовка данных
        List<Map<String, Object>> data = List.of(
                Map.of("id", 1, "name", "User1"),
                Map.of("id", 2, "name", "User2")
        );

        // Выполнение экспорта с флагом архивации через контекст
        ExportResource resource = TableDataLoader.<Map<String, Object>>create(context)
                .useToGetData(param -> data)
                .archiveResult()
                .build(CsvFileExporter.class, "report");

        // 1. Проверка метаданных ресурса
        assertNotNull(resource);
        assertEquals("report.zip", resource.fileName());

        // 2. Проверка содержимого ZIP
        try (InputStream inputStream = resource.stream();
             ZipInputStream zis = new ZipInputStream(inputStream)) {
            
            ZipEntry entry = zis.getNextEntry();
            assertNotNull(entry, "В архиве должен быть хотя бы один файл");
            assertEquals("report.csv", entry.getName());

            byte[] buffer = zis.readAllBytes();
            String csvContent = new String(buffer);
            assertTrue(csvContent.contains("User1"));
            
            assertNull(zis.getNextEntry(), "В архиве не должно быть других файлов");
        }
    }
}
