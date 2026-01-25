package su.erik.tabledataloader.exporter.factory;

import com.puls.centralpricing.common.exception.StandardFault;
import su.erik.tabledataloader.exporter.FileExporter;

import java.lang.reflect.Constructor;
import java.util.function.Consumer;

public class ReflectionFileExporterFactory implements FileExporterFactory {

    @Override
    public FileExporter createExporter(Iterable<?> data, Class<? extends FileExporter> exporterClass) {
        try {
            Constructor<? extends FileExporter> constructor = exporterClass.getConstructor(Iterable.class);
            return constructor.newInstance(data);
        } catch (Exception exception) {
            throw new StandardFault(exception);
        }
    }

    @Override
    public <T> FileExporter createExporter(Iterable<T> data, Class<? extends FileExporter> exporterClass, Consumer<T> consumer) {
        try {
            Constructor<? extends FileExporter> constructor = exporterClass.getConstructor(Iterable.class, Consumer.class);
            return constructor.newInstance(data, consumer);
        } catch (Exception exception) {
            throw new StandardFault(exception);
        }
    }
}