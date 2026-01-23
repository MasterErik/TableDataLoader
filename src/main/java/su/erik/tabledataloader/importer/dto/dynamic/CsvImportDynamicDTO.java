package su.erik.tabledataloader.importer.dto.dynamic;

public abstract class CsvImportDynamicDTO<T> extends ImportDynamicDTO<T> {

    public CsvImportDynamicDTO(Integer dynamicValueSize) {
        super(dynamicValueSize);
    }
}
