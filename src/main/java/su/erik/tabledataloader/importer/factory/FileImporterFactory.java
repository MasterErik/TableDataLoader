package su.erik.tabledataloader.importer.factory;

import su.erik.tabledataloader.importer.FileImporter;
import su.erik.tabledataloader.importer.ImportMapper;
import java.util.Map;

public interface FileImporterFactory {
    /**
     * Создает экземпляр импортера.
     * Обычно использует Reflection для создания экземпляра importerClass.
     */
    <T> FileImporter createImporter(
            Class<? extends FileImporter> importerClass,
            Class<T> importDTOClass,
            ImportMapper<T> importMapper,
            Map<String, Object> customFilters
    );
}
