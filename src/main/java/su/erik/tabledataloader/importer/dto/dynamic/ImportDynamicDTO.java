package su.erik.tabledataloader.importer.dto.dynamic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Базовый класс для DTO с динамическими колонками.
 */
public abstract class ImportDynamicDTO<T> {
    
    public final List<T> dynamicValues;

    /**
     * @param dynamicValueSize количество динамических колонок (размер "хвоста").
     */
    public ImportDynamicDTO(Integer dynamicValueSize) {
        if (dynamicValueSize != null && dynamicValueSize > 0) {
            this.dynamicValues = new ArrayList<>(Collections.nCopies(dynamicValueSize, null));
        } else {
            this.dynamicValues = new ArrayList<>();
        }
    }

    /**
     * Метод для установки значения динамической колонки.
     * @param value строковое значение из ячейки.
     * @param index индекс динамической колонки (0 = первая динамическая колонка).
     */
    public abstract void setterDynamicValue(String value, Integer index);
}
