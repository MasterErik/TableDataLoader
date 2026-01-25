package su.erik.tabledataloader.spi;

import su.erik.tabledataloader.config.EnumLoaderType;
import su.erik.tabledataloader.importer.loader.ZipFileLoader;

import java.util.List;

public class ZipLoaderDescriptor implements LoaderDescriptor {
    @Override
    public EnumLoaderType getType() {
        return EnumLoaderType.LOADER;
    }

    @Override
    public List<String> getSupportedExtensions() {
        return List.of("zip");
    }

    @Override
    public Class<?> getComponentClass() {
        return ZipFileLoader.class;
    }
}
