package su.erik.tabledataloader.exporter.csv;

import su.erik.tabledataloader.exporter.AbstractFileExporter;
import su.erik.tabledataloader.exporter.ExportedFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Экспортер данных в формат CSV.
 */
public class CsvFileExporter<T> extends AbstractFileExporter<T> {

    private static final String SEPARATOR = ";";
    private static final String NEW_LINE = "\n";

    public CsvFileExporter(Iterable<T> data) {
        super(data);
    }

    public CsvFileExporter(Iterable<T> data, Consumer<T> consumer) {
        super(data, consumer);
    }

    @Override
    public ExportedFile export() {
        StringBuilder csvContent = new StringBuilder();
        Iterator<T> iterator = data.iterator();

        if (iterator.hasNext()) {
            T firstItem = iterator.next();
            processItem(firstItem); // Обрабатываем первый элемент (он уже "съеден" итератором)

            // 1. Определяем заголовки
            List<String> headers = resolveHeaders(firstItem);
            
            // 2. Пишем заголовки
            csvContent.append(String.join(SEPARATOR, headers)).append(NEW_LINE);

            // 3. Пишем данные первого элемента
            writeRow(csvContent, extractRowValues(firstItem, headers));

            // 4. Пишем остальные данные
            while (iterator.hasNext()) {
                T item = iterator.next();
                processItem(item);
                writeRow(csvContent, extractRowValues(item, headers));
            }
        }

        byte[] bytes = csvContent.toString().getBytes(StandardCharsets.UTF_8);

        return new ExportedFile() {
            @Override
            public InputStream getInputStream() throws IOException {
                return new ByteArrayInputStream(bytes);
            }

            @Override
            public long contentLength() throws IOException {
                return bytes.length;
            }
        };
    }

    private void writeRow(StringBuilder builder, List<Object> values) {
        String rowString = values.stream()
                .map(this::formatValue)
                .collect(Collectors.joining(SEPARATOR));
        builder.append(rowString).append(NEW_LINE);
    }

    private String formatValue(Object value) {
        if (value == null) return "";
        String stringValue = value.toString();
        // Экранирование: если есть разделитель или перенос строки, оборачиваем в кавычки
        if (stringValue.contains(SEPARATOR) || stringValue.contains("\n") || stringValue.contains("\"")) {
            return "\"" + stringValue.replace("\"", "\"\"") + "\"";
        }
        return stringValue;
    }

    @Override
    public String getFullFileName(String fileName) {
        return fileName + ".csv";
    }
}