package su.erik.tabledataloader.exporter;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Базовый класс для экспортеров данных.
 * Инкапсулирует логику работы с источниками данных и рефлексию DTO.
 */
public abstract class AbstractFileExporter<T> implements FileExporter {

    protected final Iterable<T> data;
    protected final Consumer<T> forEachConsumer;
    protected final List<Object> headerOrder = new ArrayList<>();
    protected final Map<Object, Method> getterMethods = new HashMap<>();

    public AbstractFileExporter(Iterable<T> data) {
        this(data, null);
    }

    public AbstractFileExporter(Iterable<T> data, Consumer<T> forEachConsumer) {
        this.data = data;
        this.forEachConsumer = forEachConsumer;
    }

    protected void retrieveHeadersFromData(T entity) {
        if (headerOrder.isEmpty()) {
            getHeadersFromEntity(entity).forEach(headerOrder::add);
        }
    }

    protected void initGetterMethods(T item) {
        if (getterMethods.isEmpty() && !(item instanceof Map)) {
            Class<?> clazz = item.getClass();
            for (Method method : clazz.getMethods()) {
                if (method.getName().startsWith("get") && method.getParameterCount() == 0 && !method.getName().equals("getClass")) {
                    String fieldName = method.getName().substring(3);
                    // Учитываем camelCase: GetName -> Name или name? В старом коде было substring(3).
                    getterMethods.put(fieldName, method);
                }
            }
        }
    }

    protected Object getFieldValue(T object, Object fieldName) {
        if (object instanceof Map) {
            return ((Map<?, ?>) object).get(fieldName);
        } else {
            Method method = getterMethods.get(fieldName);
            if (method == null) {
                // Пытаемся найти по lowercase если не нашли (для гибкости)
                method = getterMethods.get(fieldName.toString().toLowerCase());
            }
            if (method != null) {
                try {
                    return method.invoke(object);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException("Error invoking getter for field: " + fieldName, e);
                }
            }
            return null;
        }
    }

    protected Stream<?> getHeadersFromEntity(T entity) {
        if (entity instanceof Map) {
            return ((Map<?, ?>) entity).keySet().stream();
        } else {
            return Arrays.stream(entity.getClass().getDeclaredFields())
                    .map(Field::getName);
        }
    }

    protected void processItem(T item) {
        if (forEachConsumer != null) {
            forEachConsumer.accept(item);
        }
    }
}
