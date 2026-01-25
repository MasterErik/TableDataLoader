package su.erik.tabledataloader.importer.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Базовый класс для моделей с динамическими колонками.
 */
public abstract class ImportDynamic<T> extends ImportBase {
    
    public final List<T> dynamicValues;

    public ImportDynamic(Integer dynamicValueSize) {
        if (dynamicValueSize != null && dynamicValueSize > 0) {
            this.dynamicValues = new ArrayList<>(Collections.nCopies(dynamicValueSize, null));
        } else {
            this.dynamicValues = new ArrayList<>();
        }
    }

    public abstract void setterDynamicValue(String value, Integer index);
}
