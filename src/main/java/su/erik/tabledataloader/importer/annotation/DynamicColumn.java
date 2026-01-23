package su.erik.tabledataloader.importer.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация для поддержки динамических колонок при импорте.
 * Указывает индекс, с которого начинаются динамические колонки.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DynamicColumn {
    int startsFromIndex() default 0;
}
