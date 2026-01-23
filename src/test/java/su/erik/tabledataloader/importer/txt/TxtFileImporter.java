package su.erik.tabledataloader.importer.txt;

import su.erik.tabledataloader.importer.AbstractFileImporter;
import su.erik.tabledataloader.importer.ImportMapper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

public class TxtFileImporter extends AbstractFileImporter<TxtDto> {

    public TxtFileImporter(Class<TxtDto> importDTOClass, ImportMapper<TxtDto> importMapper, Map<String, Object> customFilters) {
        super(importDTOClass, importMapper, customFilters);
    }

    @Override
    protected Iterable<TxtDto> iteratorBuilder(InputStream inputStream) {
        return () -> new Iterator<>() {
            private final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            private String nextLine = null;

            {
                advance();
            }

            private void advance() {
                try {
                    nextLine = reader.readLine();
                } catch (Exception e) {
                    nextLine = null;
                    throw new RuntimeException(e);
                }
            }

            @Override
            public boolean hasNext() {
                return nextLine != null;
            }

            @Override
            public TxtDto next() {
                if (nextLine == null) throw new NoSuchElementException();
                TxtDto dto = new TxtDto(nextLine);
                advance();
                return dto;
            }
        };
    }
}
