package su.erik.tabledataloader.spi;

import su.erik.tabledataloader.param.MapParam;

/**
 * Service Provider Interface (SPI) для заполнения контекста запроса.
 * Позволяет библиотеке получать данные о пользователе, ролях и пагинации
 * из внешнего окружения (например, Spring Security, HttpServletRequest),
 * не имея прямых зависимостей от этого окружения.
 */
public interface MapParamProvider {

    /**
     * Заполняет MapParam данными из текущего контекста.
     * @param mapParam объект параметров, который нужно обогатить.
     */
    void fill(MapParam mapParam);
}
