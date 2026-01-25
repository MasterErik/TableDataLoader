package su.erik.tabledataloader;

import com.puls.centralpricing.common.exception.BaseFaultException;
import com.puls.centralpricing.common.exception.Error;
import su.erik.tabledataloader.config.EnumLoaderType;
import su.erik.tabledataloader.exporter.FileExporter;
import su.erik.tabledataloader.exporter.factory.FileExporterFactory;
import su.erik.tabledataloader.importer.ImportMapper;
import su.erik.tabledataloader.importer.factory.FileImporterFactory;
import su.erik.tabledataloader.importer.loader.FileLoader;
import su.erik.tabledataloader.spi.LoaderDescriptor;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Реестр загрузчиков и экспортеров.
 */
public class LoaderRegistry {

    private final Map<String, Class<? extends FileLoader>> loaderClasses = new HashMap<>();
    private final Map<String, Class<? extends FileExporter>> exporterClasses = new HashMap<>();

    private FileImporterFactory loaderFactory;
    private FileExporterFactory exporterFactory;

    public LoaderRegistry() {
        refresh();
    }

    /**
     * Перезагружает сервисы SPI (дескрипторы и фабрики).
     */
    public synchronized void refresh() {
        loaderClasses.clear();
        exporterClasses.clear();

        ServiceLoader<LoaderDescriptor> descriptors = ServiceLoader.load(LoaderDescriptor.class);
        for (LoaderDescriptor descriptor : descriptors) {
            registerDescriptor(descriptor);
        }

        ServiceLoader<FileImporterFactory> importerFactories = ServiceLoader.load(FileImporterFactory.class);
        this.loaderFactory = importerFactories.findFirst().orElse(null);

        ServiceLoader<FileExporterFactory> exporterFactories = ServiceLoader.load(FileExporterFactory.class);
        this.exporterFactory = exporterFactories.findFirst().orElse(null);
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

    public void setLoaderFactory(FileImporterFactory factory) { this.loaderFactory = factory; }
    public void setExporterFactory(FileExporterFactory factory) { this.exporterFactory = factory; }

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
        // Мы больше не можем передавать "this" как ImporterRegistry (имя изменилось)
        // Но ZipFileLoader ожидает реестр в фильтрах.
        filters.put("loaderRegistry", this);
        if (loaderFactory == null) throw new IllegalStateException("FileImporterFactory not initialized");
        return loaderFactory.createImporter(loaderClass, dtoClass, mapper, filters);
    }

    public <T> FileExporter createExporter(Iterable<T> data, Class<? extends FileExporter> viewClass) {
        if (exporterFactory == null) throw new IllegalStateException("FileExporterFactory not initialized");
        return exporterFactory.createExporter(data, viewClass);
    }

    public <T> FileExporter createExporter(Iterable<T> data, Class<? extends FileExporter> viewClass, java.util.function.Consumer<T> consumer) {
        if (exporterFactory == null) throw new IllegalStateException("FileExporterFactory not initialized");
        return exporterFactory.createExporter(data, viewClass, consumer);
    }
}
