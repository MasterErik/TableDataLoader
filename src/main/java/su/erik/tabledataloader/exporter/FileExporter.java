package su.erik.tabledataloader.exporter;

import java.io.IOException;

public interface FileExporter {
    /**
     * Выполняет экспорт данных.
     * Возвращает абстракцию файла, а не Spring Resource.
     */
    ExportedFile export() throws IOException;

    /**
     * Формирует полное имя файла (например, добавляет расширение .xlsx).
     */
    String getFullFileName(String baseFileName);
}
