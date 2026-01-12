package su.erik.tabledataloader.exporter.factory;

import su.erik.tabledataloader.exporter.FileExporter; // Предполагаемый интерфейс экспортера
import java.util.function.Consumer;

public interface FileExporterFactory {

    FileExporter createExporter(Iterable<?> data, Class<? extends FileExporter> viewClass);

    <T> FileExporter createExporter(Iterable<T> data, Class<? extends FileExporter> viewClass, Consumer<T> consumer);
}
