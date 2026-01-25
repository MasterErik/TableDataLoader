package su.erik.tabledataloader.spi;

import su.erik.tabledataloader.config.EnumLoaderType;
import su.erik.tabledataloader.exporter.csv.CsvFileExporter;

import java.util.List;

public class CsvExporterDescriptor implements LoaderDescriptor {
    @Override
    public EnumLoaderType getType() {
        return EnumLoaderType.EXPORTER;
    }

    @Override
    public List<String> getSupportedExtensions() {
        return List.of("csv");
    }

    @Override
    public Class<?> getComponentClass() {
        return CsvFileExporter.class;
    }
}
