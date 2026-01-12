package su.erik.tabledataloader.mocks;

import su.erik.tabledataloader.importer.FileImporter;
import su.erik.tabledataloader.importer.ImportMapper;
import su.erik.tabledataloader.importer.dto.ImportResultDTO;
import su.erik.tabledataloader.importer.factory.FileImporterFactory;

import java.io.InputStream;
import java.util.Map;

public class MockFileImporterFactory implements FileImporterFactory {

    @Override
    public <T> FileImporter createImporter(Class<? extends FileImporter> importerClass, Class<T> importDTOClass, ImportMapper<T> importMapper, Map<String, Object> customFilters) {
        // Возвращаем заглушку импортера, которая всегда возвращает успех
        return new FileImporter() {
            @Override
            public ImportResultDTO importFile(InputStream inputStream, String name, long size, String entity, Long userId) {
                // Эмуляция работы: читаем поток (чтобы не было ошибок), возвращаем фиктивный ID
                try {
                    inputStream.readAllBytes();
                } catch (Exception ignored) {}

                return new ImportResultDTO(123L, 10); // ID=123, Count=10
            }
        };
    }
}
