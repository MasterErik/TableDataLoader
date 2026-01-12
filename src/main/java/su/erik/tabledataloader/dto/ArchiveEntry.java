package su.erik.tabledataloader.dto;

import java.io.InputStream;

/**
 * Объект, представляющий файл внутри архива.
 *
 * @param name Имя (путь) файла в архиве.
 * @param size Размер в байтах (если известен, иначе -1).
 * @param content Поток данных для чтения содержимого.
 */
public record ArchiveEntry(String name, long size, InputStream content) {}
