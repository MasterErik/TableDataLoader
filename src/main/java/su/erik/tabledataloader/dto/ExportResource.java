package su.erik.tabledataloader.dto;

import java.io.InputStream;

public record ExportResource(
        String fileName,
        String contentType,
        InputStream stream,
        long size,
        LoaderHttpStatus status
) {
    public ExportResource(String fileName, String contentType, InputStream stream, long size) {
        this(fileName, contentType, stream, size, LoaderHttpStatus.OK);
    }
}
