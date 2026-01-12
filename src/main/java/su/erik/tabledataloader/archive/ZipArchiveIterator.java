package su.erik.tabledataloader.archive;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Высокопроизводительный итератор для ZIP архивов.
 */
public class ZipArchiveIterator extends AbstractArchiveIterator {

    private final ZipInputStream zipStream;

    public ZipArchiveIterator(InputStream inputStream) throws IOException {
        this.zipStream = new ZipInputStream(new BufferedInputStream(inputStream));
        this.currentEntry = zipStream.getNextEntry();
    }

    @Override
    protected InputStream getCurrentStream() {
        return zipStream;
    }

    @Override
    protected String getCurrentEntryName() {
        return ((ZipEntry) currentEntry).getName();
    }

    @Override
    protected long getCurrentEntrySize() {
        return ((ZipEntry) currentEntry).getSize();
    }

    @Override
    protected void moveToNextEntry() throws IOException {
        currentEntry = zipStream.getNextEntry();
    }

    @Override
    protected void prepareForNext() throws IOException {
        if (currentEntry != null) {
            zipStream.closeEntry();
        }
    }

    @Override
    public void close() throws IOException {
        zipStream.close();
    }
}
