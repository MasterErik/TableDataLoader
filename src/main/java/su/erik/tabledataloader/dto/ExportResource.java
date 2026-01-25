package su.erik.tabledataloader.dto;

import java.io.InputStream;

/**
 * Ресурс для экспорта.
 * Реализует AutoCloseable для гарантированного закрытия потока.
 */
public record ExportResource(
        String fileName,
        String contentType,
        InputStream stream,
        long size,
        LoaderHttpStatus status
) implements AutoCloseable {
    
    public ExportResource(String fileName, String contentType, InputStream stream, long size) {
        this(fileName, contentType, stream, size, LoaderHttpStatus.OK);
    }

    @Override
    public void close() throws Exception {
        if (stream != null) {
            stream.close();
        }
    }
}