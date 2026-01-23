package su.erik.tabledataloader.importer.txt;

import su.erik.tabledataloader.importer.FileImporter;
import su.erik.tabledataloader.spi.FileImporterDescriptor;
import java.util.List;

public class TxtFileImporterDescriptor implements FileImporterDescriptor {
    @Override
    public List<String> getSupportedExtensions() {
        return List.of("txt");
    }

    @Override
    public Class<? extends FileImporter> getImporterClass() {
        return TxtFileImporter.class;
    }
}
