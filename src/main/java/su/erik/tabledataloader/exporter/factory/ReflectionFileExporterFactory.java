package su.erik.tabledataloader.exporter.factory;

import su.erik.tabledataloader.exporter.FileExporter;
import java.util.function.Consumer;

public class ReflectionFileExporterFactory implements FileExporterFactory {
    @Override
    public FileExporter createExporter(Iterable<?> data, Class<? extends FileExporter> viewClass) {
        throw new UnsupportedOperationException("Standard exporter not implemented yet");
    }

    @Override
    public <T> FileExporter createExporter(Iterable<T> data, Class<? extends FileExporter> viewClass, Consumer<T> consumer) {
        throw new UnsupportedOperationException("Standard exporter not implemented yet");
    }
}
