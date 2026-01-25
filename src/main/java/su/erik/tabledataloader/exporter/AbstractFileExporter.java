package su.erik.tabledataloader.exporter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Базовый класс для экспортеров.
 * Ответственность: Интроспекция объектов (извлечение списка заголовков и значений полей).
 */
public abstract class AbstractFileExporter<T> implements FileExporter {

    protected final Iterable<T> data;
    protected final Consumer<T> forEachConsumer;

    // Кеш методов для рефлексии, чтобы не искать их для каждой строки
    private final Map<String, Method> methodCache = new HashMap<>();
    private final Map<String, Field> fieldCache = new HashMap<>();

    public AbstractFileExporter(Iterable<T> data) {
        this(data, null);
    }

    public AbstractFileExporter(Iterable<T> data, Consumer<T> forEachConsumer) {
        this.data = data;
        this.forEachConsumer = forEachConsumer;
    }

    /**
     * Вызывает consumer для постобработки элемента (если задан).
     */
    protected void processItem(T item) {
        if (forEachConsumer != null) {
            forEachConsumer.accept(item);
        }
    }

    /**
     * Определяет список заголовков на основе первого элемента данных или класса.
     */
    protected List<String> resolveHeaders(T sampleItem) {
        if (sampleItem instanceof Map) {
            return new ArrayList<>(((Map<?, ?>) sampleItem).keySet()).stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
        } else {
            // Для POJO берем поля класса
            // Можно улучшить: добавить поддержку аннотаций @CsvBindByName для порядка и имен
            return Arrays.stream(sampleItem.getClass().getDeclaredFields())
                    .map(Field::getName)
                    .filter(name -> !name.startsWith("this$")) // Исключаем синтетические поля
                    .collect(Collectors.toList());
        }
    }

    /**
     * Извлекает значения полей для списка заголовков.
     */
    protected List<Object> extractRowValues(T item, List<String> headers) {
        List<Object> values = new ArrayList<>(headers.size());
        for (String header : headers) {
            values.add(getValue(item, header));
        }
        return values;
    }

    private Object getValue(T item, String fieldName) {
        if (item == null) return "";
        if (item instanceof Map) {
            return ((Map<?, ?>) item).get(fieldName);
        }

        try {
            // 1. Пытаемся через геттер (cached)
            Method method = getCachedMethod(item.getClass(), fieldName);
            if (method != null) {
                return method.invoke(item);
            }

            // 2. Пытаемся через поле (cached)
            Field field = getCachedField(item.getClass(), fieldName);
            if (field != null) {
                return field.get(item);
            }
        } catch (Exception e) {
            // Логируем или игнорируем ошибку доступа
        }
        return "";
    }

    private Method getCachedMethod(Class<?> clazz, String fieldName) {
        String key = clazz.getName() + "." + fieldName;
        if (methodCache.containsKey(key)) return methodCache.get(key);

        String getterName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        try {
            Method method = clazz.getMethod(getterName);
            methodCache.put(key, method);
            return method;
        } catch (NoSuchMethodException e) {
            methodCache.put(key, null);
            return null;
        }
    }

    private Field getCachedField(Class<?> clazz, String fieldName) {
        String key = clazz.getName() + "." + fieldName;
        if (fieldCache.containsKey(key)) return fieldCache.get(key);

        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            fieldCache.put(key, field);
            return field;
        } catch (NoSuchFieldException e) {
            fieldCache.put(key, null);
            return null;
        }
    }
}