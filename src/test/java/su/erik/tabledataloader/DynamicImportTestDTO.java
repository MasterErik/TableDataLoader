package su.erik.tabledataloader;

import su.erik.tabledataloader.importer.annotation.DynamicColumn;
import su.erik.tabledataloader.importer.annotation.ImporterBindByIndex;
import su.erik.tabledataloader.importer.dto.dynamic.CsvImportDynamicDTO;

@DynamicColumn(startsFromIndex = 2)
public class DynamicImportTestDTO extends CsvImportDynamicDTO<Double> {
    private Long staticCode;
    private String staticName;

    public DynamicImportTestDTO(Integer dynamicValueSize) {
        super(dynamicValueSize);
    }

    @ImporterBindByIndex(0)
    public void setStaticCode(String value) {
        this.staticCode = Long.valueOf(value);
    }

    @ImporterBindByIndex(1)
    public void setStaticName(String value) {
        this.staticName = value;
    }

    @Override
    public void setterDynamicValue(String value, Integer index) {
        this.dynamicValues.set(index, Double.valueOf(value));
    }

    public Long getStaticCode() { return staticCode; }
    public String getStaticName() { return staticName; }
}
