package su.erik.tabledataloader;

import su.erik.tabledataloader.importer.annotation.DynamicColumn;
import su.erik.tabledataloader.importer.annotation.ImporterBindByIndex;
import su.erik.tabledataloader.importer.model.ImportDynamic;

@DynamicColumn(startsFromIndex = 3)
public class DynamicImportTestDTO extends ImportDynamic<Double> {

    private Long id;
    private String code;
    private String name;

    public DynamicImportTestDTO(Integer size) { super(size); }

    @ImporterBindByIndex(0)
    public void setId(String val) { this.id = Long.valueOf(val); }
    @ImporterBindByIndex(1)
    public void setCode(String val) { this.code = val; }
    @ImporterBindByIndex(2)
    public void setName(String val) { this.name = val; }

    @Override
    public void setterDynamicValue(String value, Integer index) {
        if (value != null && !value.isEmpty()) {
            this.dynamicValues.set(index, Double.valueOf(value));
        }
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
}