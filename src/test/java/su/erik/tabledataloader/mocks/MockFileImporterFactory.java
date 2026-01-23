package su.erik.tabledataloader.mocks;

import su.erik.tabledataloader.importer.FileImporter;
import su.erik.tabledataloader.importer.ImportMapper;
import su.erik.tabledataloader.importer.factory.FileImporterFactory;
import su.erik.tabledataloader.importer.factory.ReflectionFileImporterFactory;

import java.util.Map;

/**
 * Мок-фабрика, которая теперь использует Reflection для создания реальных импортеров в тестах.
 * Это позволяет тестировать интеграцию с CsvFileImporter через TableDataLoader.
 */
public class MockFileImporterFactory implements FileImporterFactory {

    private final ReflectionFileImporterFactory delegate = new ReflectionFileImporterFactory();

    @Override
    public <T> FileImporter createImporter(
            Class<? extends FileImporter> importerClass,
            Class<T> importDTOClass,
            ImportMapper<T> importMapper,
            Map<String, Object> customFilters) {
        
        // Если передан конкретный класс импортера (например, CsvFileImporter), создаем его реально.
        // Иначе возвращаем заглушку.
        if (importerClass != null && !importerClass.isInterface()) {
            return delegate.createImporter(importerClass, importDTOClass, importMapper, customFilters);
        }

        // Default Mock behavior
        return (inputStream, name, size, entity, userId) -> {
            try {
                inputStream.readAllBytes();
            } catch (Exception ignored) {}
            return new su.erik.tabledataloader.importer.dto.ImportResultDTO(123L, 10);
        };
    }
}