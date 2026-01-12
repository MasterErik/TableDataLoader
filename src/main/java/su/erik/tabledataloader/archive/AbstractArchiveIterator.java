package su.erik.tabledataloader.archive;

import com.puls.centralpricing.common.exception.StandardFault;
import su.erik.tabledataloader.dto.ArchiveEntry;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Базовый класс для потоковой обработки архивов различных типов.
 */
public abstract class AbstractArchiveIterator implements Iterator<ArchiveEntry>, AutoCloseable {

    protected Object currentEntry;
    private boolean isCurrentEntryConsumed = false;

    /**
     * Возвращает поток текущего элемента архива.
     */
    protected abstract InputStream getCurrentStream() throws IOException;

    /**
     * Возвращает имя текущего элемента.
     */
    protected abstract String getCurrentEntryName();

    /**
     * Возвращает размер текущего элемента.
     */
    protected abstract long getCurrentEntrySize();

    /**
     * Перемещает указатель архива к следующему элементу.
     */
    protected abstract void moveToNextEntry() throws IOException;

    @Override
    public boolean hasNext() {
        return currentEntry != null;
    }

    @Override
    public ArchiveEntry next() {
        if (!hasNext()) {
            throw new NoSuchElementException("Archive is empty or all entries processed");
        }

        try {
            // Если предыдущий поток не был вычитан, подготавливаемся к переходу
            prepareForNext();

            ArchiveEntry entry = new ArchiveEntry(
                    getCurrentEntryName(),
                    getCurrentEntrySize(),
                    new ShieldedInputStream(getCurrentStream())
            );

            isCurrentEntryConsumed = false;
            moveToNextEntry();

            return entry;
        } catch (IOException e) {
            throw new StandardFault(e);
        }
    }

    protected void prepareForNext() throws IOException {
        // Логика пропуска остатков текущего файла (реализуется в Zip/Rar подтипах если нужно)
    }

    /**
     * Защитный поток, который игнорирует close(), 
     * чтобы пользователь не закрыл весь архив случайно.
     */
    protected static class ShieldedInputStream extends FilterInputStream {
        public ShieldedInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() {
            // Игнорируем закрытие, чтобы оставить поток архива открытым
        }
    }
}
