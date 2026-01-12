package su.erik.tabledataloader.dto;

import java.io.IOException;
import java.io.InputStream;

/**
 * Абстракция входящего файла.
 * Позволяет библиотеке работать с файлами без зависимости от Spring MultipartFile.
 */
public interface InputFile {

    /**
     * Возвращает поток для чтения содержимого файла.
     */
    InputStream getInputStream() throws IOException;

    /**
     * Возвращает оригинальное имя файла (например, "report_2023.xlsx").
     */
    String getOriginalFilename();

    /**
     * Возвращает размер файла в байтах.
     */
    long getSize();
}
