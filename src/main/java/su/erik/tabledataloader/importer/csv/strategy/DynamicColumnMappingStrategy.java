package su.erik.tabledataloader.importer.csv.strategy;

import com.opencsv.bean.MappingStrategy;
import com.opencsv.exceptions.CsvBeanIntrospectionException;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import su.erik.tabledataloader.importer.annotation.DynamicColumn;
import su.erik.tabledataloader.importer.annotation.ImporterBindByIndex;
import su.erik.tabledataloader.importer.dto.dynamic.ImportDynamicDTO;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Стратегия маппинга для динамических колонок.
 * Поддерживает создание объектов, наследуемых от ImportDynamicDTO,
 * с передачей размера динамической части в конструктор.
 */
public class DynamicColumnMappingStrategy<T> implements MappingStrategy<T> {

    private Class<? extends T> type;
    private int startsFromIndex;
    private final Map<Integer, Method> indexMethods = new HashMap<>();
    private Constructor<? extends T> constructor;

    @Override
    public void captureHeader(com.opencsv.CSVReader reader) throws java.io.IOException {
        // Заголовки нам не важны для позиционного маппинга, но метод обязателен
    }

    @Override
    public String[] generateHeader(T bean) throws CsvRequiredFieldEmptyException {
        return new String[0]; // Экспорт пока не реализуем
    }

    @Override
    public T populateNewBean(String[] line) throws CsvBeanIntrospectionException, CsvRequiredFieldEmptyException, CsvDataTypeMismatchException, CsvConstraintViolationException {
        try {
            int totalColumns = line.length;
            int dynamicSize = Math.max(0, totalColumns - startsFromIndex);

            // 1. Создаем экземпляр DTO
            T bean = constructor.newInstance(dynamicSize);

            // 2. Маппим колонки
            for (int i = 0; i < totalColumns; i++) {
                String value = line[i];
                if (value == null) continue;

                if (i < startsFromIndex) {
                    // Статическая часть: ищем метод по индексу
                    Method method = indexMethods.get(i);
                    if (method != null) {
                        method.invoke(bean, value);
                    }
                } else {
                    // Динамическая часть
                    if (bean instanceof ImportDynamicDTO) {
                        int dynamicIndex = i - startsFromIndex;
                        ((ImportDynamicDTO<?>) bean).setterDynamicValue(value, dynamicIndex);
                    }
                }
            }
            return bean;

        } catch (Exception e) {
            Throwable cause = (e instanceof java.lang.reflect.InvocationTargetException) ? e.getCause() : e;
            String msg = "Error creating bean " + (type != null ? type.getName() : "null") +
                         ". Constructor: " + (constructor != null ? "found" : "null") +
                         ". Cause: " + cause.getMessage();
            throw new CsvBeanIntrospectionException(msg);
        }
    }

    @Override
    public String[] transmuteBean(T bean) throws CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {
        return new String[0];
    }

    @Override
    public void setType(Class<? extends T> type) throws CsvBeanIntrospectionException {
        this.type = type;
        init();
    }

    // --- Initialization ---

    private void init() {
        // 1. Читаем аннотацию @DynamicColumn
        DynamicColumn annotation = type.getAnnotation(DynamicColumn.class);
        if (annotation == null) {
            throw new IllegalStateException("Class " + type.getName() + " must be annotated with @DynamicColumn");
        }
        this.startsFromIndex = annotation.startsFromIndex();

        // 2. Ищем конструктор с параметром (Integer)
        try {
            this.constructor = type.getConstructor(Integer.class);
        } catch (NoSuchMethodException e) {
             // Попробуем int (primitive)
            try {
                this.constructor = type.getConstructor(int.class);
            } catch (NoSuchMethodException ex) {
                throw new IllegalStateException("Class " + type.getName() + " must have a constructor accepting (Integer dynamicSize)");
            }
        }

        // 3. Кэшируем методы с @ImporterBindByIndex
        for (Method method : type.getMethods()) {
            if (method.isAnnotationPresent(ImporterBindByIndex.class)) {
                int index = method.getAnnotation(ImporterBindByIndex.class).value();
                indexMethods.put(index, method);
            }
        }
    }
}
