package su.erik.tabledataloader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import su.erik.tabledataloader.param.MapParam;

import static org.junit.jupiter.api.Assertions.*;

class TableDataLoaderParamInitTest {

    @Test
    @DisplayName("INIT: Проверка автоматической инициализации параметров через SPI")
    void testParamInitialization() {
        // 1. Создаем лоадер
        // Конструктор вызывает ServiceLoader -> MockMapParamProvider.fill()
        TableDataLoader<Object> loader = TableDataLoader.create();

        // 2. Получаем параметры
        MapParam param = loader.getMapParam();

        // 3. Проверки
        assertNotNull(param, "MapParam должен быть создан");

        // Проверяем работу MockMapParamProvider (он всегда ставит userId=999)
        assertEquals(999L, param.getUserId(),
                "Провайдер не сработал: userId не установлен");

        // Проверяем, что БЕЗ явного вызова setLimit() и БЕЗ http-контекста, limit остается null (или дефолтным)
        assertNull(param.getLimit(), "Limit должен быть null, пока он не задан явно или через HTTP");
    }
}
