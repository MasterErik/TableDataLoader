package su.erik.tabledataloader.exporter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipExporter implements FileExporter {

    private final FileExporter delegate;
    private final String fileNameInsideArchive;

    public ZipExporter(FileExporter delegate, String fileNameInsideArchive) {
        this.delegate = delegate;
        this.fileNameInsideArchive = fileNameInsideArchive;
    }

    @Override
    public ExportedFile export() {
        try (ExportedFile innerFile = delegate.export()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {

                String entryName = delegate.getFullFileName(fileNameInsideArchive);
                ZipEntry entry = new ZipEntry(entryName);
                zos.putNextEntry(entry);

                try (InputStream is = innerFile.getInputStream()) {
                    is.transferTo(zos);
                }
                zos.closeEntry();
            }

            byte[] zippedBytes = baos.toByteArray();
            return new ExportedFile() {
                @Override
                public InputStream getInputStream() {
                    return new ByteArrayInputStream(zippedBytes);
                }

                @Override
                public long contentLength() {
                    return zippedBytes.length;
                }
            };
        } catch (Exception e) {
            throw new RuntimeException("Error while archiving export result", e);
        }
    }

    @Override
    public String getFullFileName(String baseName) {
        return baseName + ".zip";
    }
}
