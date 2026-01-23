package su.erik.tabledataloader.importer.csv;

import su.erik.tabledataloader.importer.FileImporter;
import su.erik.tabledataloader.spi.FileImporterDescriptor;
import java.util.List;

public class CsvFileImporterDescriptor implements FileImporterDescriptor {
    @Override
    public List<String> getSupportedExtensions() {
        return List.of("csv");
    }

    @Override
    public Class<? extends FileImporter> getImporterClass() {
        return CsvFileImporter.class;
    }
}
