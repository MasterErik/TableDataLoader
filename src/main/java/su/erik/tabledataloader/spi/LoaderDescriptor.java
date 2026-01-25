package su.erik.tabledataloader.spi;

import su.erik.tabledataloader.config.EnumLoaderType;

import java.util.List;

/**
 * Единый интерфейс для описания компонентов (плагинов) системы.
 */
public interface LoaderDescriptor {
    /**
     * Тип компонента (Загрузчик или Экспортер).
     */
    EnumLoaderType getType();

    /**
     * Список поддерживаемых расширений.
     */
    List<String> getSupportedExtensions();

    /**
     * Класс реализации.
     */
    Class<?> getComponentClass();
}
