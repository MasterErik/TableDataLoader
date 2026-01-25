package su.erik.tabledataloader.context;

import su.erik.tabledataloader.LoaderRegistry;
import su.erik.tabledataloader.spi.MapParamProvider;

import java.util.ServiceLoader;

/**
 * Контекст выполнения TableDataLoader.
 * Хранит реестр лоадеров и провайдер параметров.
 */
public class DataLoaderContext {

    private static final DataLoaderContext DEFAULT_CONTEXT = new DataLoaderContext();

    private final LoaderRegistry loaderRegistry;
    private final MapParamProvider mapParamProvider;

    /**
     * Конструктор по умолчанию, загружающий компоненты через SPI.
     */
    public DataLoaderContext() {
        this.loaderRegistry = new LoaderRegistry();
        
        ServiceLoader<MapParamProvider> providers = ServiceLoader.load(MapParamProvider.class);
        this.mapParamProvider = providers.findFirst().orElse(null);
    }

    /**
     * Конструктор для ручного создания контекста (удобно для тестов).
     */
    public DataLoaderContext(LoaderRegistry registry, MapParamProvider provider) {
        this.loaderRegistry = registry;
        this.mapParamProvider = provider;
    }

    public static DataLoaderContext getDefault() {
        return DEFAULT_CONTEXT;
    }

    public LoaderRegistry getLoaderRegistry() {
        return loaderRegistry;
    }

    public MapParamProvider getMapParamProvider() {
        return mapParamProvider;
    }
}
