package su.erik.tabledataloader.spi;

import su.erik.tabledataloader.importer.FileImporter;
import java.util.List;

/**
 * Дескриптор для регистрации импортеров файлов через SPI.
 * Позволяет TableDataLoader автоматически выбирать нужный импортер по расширению файла.
 */
public interface FileImporterDescriptor {
    /**
     * Возвращает список расширений файлов (без точки), которые поддерживает данный импортер.
     * Например: List.of("csv", "txt")
     */
    List<String> getSupportedExtensions();

    /**
     * Возвращает класс импортера, который будет использоваться для обработки файлов.
     */
    Class<? extends FileImporter> getImporterClass();
}
