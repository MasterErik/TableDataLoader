package su.erik.tabledataloader.archive;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * Фасад для работы с архивами различных типов.
 */
public class ArchiveIterator implements Iterator<EntryModel>, AutoCloseable {

    private final AbstractIterator delegate;

    public ArchiveIterator(InputStream inputStream, String fileName) throws IOException {
        if (fileName.toLowerCase().endsWith(".zip")) {
            this.delegate = new ZipArchiveIterator(inputStream);
        } else {
            // В будущем добавим Rar и 7z
            throw new UnsupportedOperationException("Unsupported archive type: " + fileName);
        }
    }

    @Override
    public boolean hasNext() {
        return delegate.hasNext();
    }

    @Override
    public EntryModel next() {
        return delegate.next();
    }

    @Override
    public void close() throws IOException {
        try {
            delegate.close();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Error closing archive delegate", e);
        }
    }
}