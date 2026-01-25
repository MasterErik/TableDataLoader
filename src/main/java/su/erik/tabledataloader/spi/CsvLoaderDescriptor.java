package su.erik.tabledataloader.spi;

import su.erik.tabledataloader.config.EnumLoaderType;
import su.erik.tabledataloader.importer.loader.CsvFileLoader;

import java.util.List;

public class CsvLoaderDescriptor implements LoaderDescriptor {
    @Override
    public EnumLoaderType getType() {
        return EnumLoaderType.LOADER;
    }

    @Override
    public List<String> getSupportedExtensions() {
        return List.of("csv");
    }

    @Override
    public Class<?> getComponentClass() {
        return CsvFileLoader.class;
    }
}
