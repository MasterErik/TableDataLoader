package su.erik.tabledataloader.exporter;

import java.io.IOException;
import java.io.InputStream;

public interface ExportedFile {
    /**
     * Открывает поток для чтения сгенерированного файла.
     */
    InputStream getInputStream() throws IOException;

    /**
     * Возвращает размер файла в байтах (если известен), иначе -1.
     */
    long contentLength() throws IOException;
}
