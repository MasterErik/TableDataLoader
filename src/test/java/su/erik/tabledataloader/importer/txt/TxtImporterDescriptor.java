package su.erik.tabledataloader.importer.txt;

import su.erik.tabledataloader.config.EnumLoaderType;
import su.erik.tabledataloader.spi.LoaderDescriptor;
import java.util.List;

public class TxtImporterDescriptor implements LoaderDescriptor {
    @Override
    public EnumLoaderType getType() {
        return EnumLoaderType.LOADER;
    }

    @Override
    public List<String> getSupportedExtensions() {
        return List.of("txt");
    }

    @Override
    public Class<?> getComponentClass() {
        return TxtImportSpiTest.TxtFileLoader.class;
    }
}