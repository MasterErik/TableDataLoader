package su.erik.tabledataloader.importer;

import su.erik.tabledataloader.importer.dto.ImportResultDTO;
import java.io.InputStream;

public interface FileImporter {
    /**
     * Основной метод импорта потока.
     */
    ImportResultDTO importFile(InputStream inputStream, String name, long size, String entity, Long userId);
}
