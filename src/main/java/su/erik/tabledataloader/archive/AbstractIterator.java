package su.erik.tabledataloader.archive;

import com.puls.centralpricing.common.exception.StandardFault;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Базовый итератор для архивов.
 */
public abstract class AbstractIterator implements Iterator<EntryModel>, AutoCloseable {

    protected Object currentEntry;
    private boolean advanceOnNext = false;

    protected abstract InputStream getCurrentStream() throws IOException;
    protected abstract String getCurrentEntryName();
    protected abstract long getCurrentEntrySize();
    protected abstract void moveToNextEntry() throws IOException;

    // Сужаем контракт исключения до IOException для try-with-resources
    @Override
    public abstract void close() throws IOException;

    @Override
    public boolean hasNext() {
        if (advanceOnNext) {
            try {
                moveToNextEntry();
                advanceOnNext = false;
            } catch (IOException exception) {
                throw new StandardFault(exception);
            }
        }
        return currentEntry != null;
    }

    @Override
    public EntryModel next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        try {
            EntryModel entry = new EntryModel(
                    getCurrentEntryName(),
                    getCurrentEntrySize(),
                    new ShieldedInputStream(getCurrentStream())
            );
            
            advanceOnNext = true;
            return entry;
        } catch (IOException exception) {
            throw new StandardFault(exception);
        }
    }

    protected static class ShieldedInputStream extends FilterInputStream {
        public ShieldedInputStream(InputStream inputStream) { super(inputStream); }
        @Override public void close() { /* no-op */ }
    }
}