package su.erik.tabledataloader.importer.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация для привязки метода-сеттера к индексу колонки.
 * Используется совместно с @DynamicColumn.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ImporterBindByIndex {
    int value();
}
