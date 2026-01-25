package su.erik.tabledataloader;

import com.puls.centralpricing.common.exception.BaseFaultException;
import com.puls.centralpricing.common.exception.Error;
import com.puls.centralpricing.common.exception.StandardFault;
import su.erik.tabledataloader.config.EnumLoaderType;
import su.erik.tabledataloader.exporter.FileExporter;
import su.erik.tabledataloader.importer.ImportMapper;
import su.erik.tabledataloader.importer.loader.FileLoader;
import su.erik.tabledataloader.spi.LoaderDescriptor;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.function.Consumer;

/**
 * Реестр загрузчиков и экспортеров.
 * Управляет метаданными компонентов и их созданием.
 */
public class LoaderRegistry {

    private final Map<String, Class<? extends FileLoader>> loaderClasses = new HashMap<>();
    private final Map<String, Class<? extends FileExporter>> exporterClasses = new HashMap<>();
    
    public LoaderRegistry() {
        refresh();
    }

    /**
     * Перезагружает сервисы SPI (дескрипторы).
     */
    public synchronized void refresh() {
        loaderClasses.clear();
        exporterClasses.clear();

        ServiceLoader<LoaderDescriptor> descriptors = ServiceLoader.load(LoaderDescriptor.class);
        for (LoaderDescriptor descriptor : descriptors) {
            registerDescriptor(descriptor);
        }
    }

    @SuppressWarnings("unchecked")
    private void registerDescriptor(LoaderDescriptor descriptor) {
        Class<?> componentClass = descriptor.getComponentClass();
        
        if (descriptor.getType() == EnumLoaderType.LOADER) {
            if (FileLoader.class.isAssignableFrom(componentClass)) {
                for (String extension : descriptor.getSupportedExtensions()) {
                    loaderClasses.put(extension.toLowerCase(), (Class<? extends FileLoader>) componentClass);
                }
            }
        } else if (descriptor.getType() == EnumLoaderType.EXPORTER) {
            if (FileExporter.class.isAssignableFrom(componentClass)) {
                for (String extension : descriptor.getSupportedExtensions()) {
                    exporterClasses.put(extension.toLowerCase(), (Class<? extends FileExporter>) componentClass);
                }
            }
        }
    }

    public void registerLoader(String extension, Class<? extends FileLoader> loaderClass) { loaderClasses.put(extension.toLowerCase(), loaderClass); }
    public void registerExporter(String extension, Class<? extends FileExporter> exporterClass) { exporterClasses.put(extension.toLowerCase(), exporterClass); }

    public Class<? extends FileLoader> getLoaderClass(String extension) {
        if (extension == null) return null;
        return loaderClasses.get(extension.toLowerCase().replace(".", ""));
    }
    
    public Class<? extends FileExporter> getExporterClass(String extension) {
        if (extension == null) return null;
        return exporterClasses.get(extension.toLowerCase().replace(".", ""));
    }

    public <T> FileLoader createLoader(String extension, Class<T> dtoClass, ImportMapper<T> mapper, Map<String, Object> filters) {
        Class<? extends FileLoader> loaderClass = getLoaderClass(extension);
        if (loaderClass == null) throw new BaseFaultException(Error.U014, "No loader found for extension: " + extension);
        return createLoader(loaderClass, dtoClass, mapper, filters);
    }

    public <T> FileLoader createLoader(Class<? extends FileLoader> loaderClass, Class<T> dtoClass, ImportMapper<T> mapper, Map<String, Object> filters) {
        filters.put("loaderRegistry", this);
        try {
            // Ищем конструктор: (Class<?> dtoClass, ImportMapper<?> mapper, Map<String, Object> filters)
            Constructor<? extends FileLoader> constructor = loaderClass.getConstructor(Class.class, ImportMapper.class, Map.class);
            return constructor.newInstance(dtoClass, mapper, filters);
        } catch (Exception e) {
            throw new StandardFault(e);
        }
    }
    
    public <T> FileExporter createExporter(Iterable<T> data, Class<? extends FileExporter> viewClass) {
        try {
            // Ищем конструктор: (Iterable<?> data)
            Constructor<? extends FileExporter> constructor = viewClass.getConstructor(Iterable.class);
            return constructor.newInstance(data);
        } catch (Exception e) {
            throw new StandardFault(e);
        }
    }
    
    public <T> FileExporter createExporter(Iterable<T> data, Class<? extends FileExporter> viewClass, Consumer<T> consumer) {
        try {
            // Ищем конструктор: (Iterable<?> data, Consumer<?> consumer)
            Constructor<? extends FileExporter> constructor = viewClass.getConstructor(Iterable.class, Consumer.class);
            return constructor.newInstance(data, consumer);
        } catch (Exception e) {
            throw new StandardFault(e);
        }
    }
}