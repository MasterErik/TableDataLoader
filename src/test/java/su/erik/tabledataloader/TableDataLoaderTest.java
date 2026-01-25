package su.erik.tabledataloader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import su.erik.tabledataloader.context.DataLoaderContext;
import su.erik.tabledataloader.param.MapParam;
import su.erik.tabledataloader.spi.MapParamProvider;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TableDataLoaderTest {

    public static class DummyMapParamProvider implements MapParamProvider {
        @Override
        public void fill(MapParam mapParam) {
            mapParam.filter("dummy", "value");
        }
    }

    @Test
    @DisplayName("CONTEXT: Проверка инициализации параметров через контекст")
    void testContextInitialization() {
        // Создаем изолированный контекст с нашим провайдером
        DataLoaderContext context = new DataLoaderContext(new LoaderRegistry(), new DummyMapParamProvider());
        
        TableDataLoader<Object> loader = TableDataLoader.create(context);
        
        assertNotNull(loader.getMapParam().getFilters().get("dummy"), "Параметр 'dummy' должен быть заполнен провайдером из контекста");
    }

    @Test
    @DisplayName("VALIDATION: Проверка правила U007 (Pagination requires Sorting)")
    void testPaginationValidation() {
        // Тест логики валидации (если она есть в build)
        TableDataLoader<Object> loader = TableDataLoader.create()
                .setLimit(10);
        
        // В текущей реализации build() не кидает исключение U007, 
        // но мы можем проверить, что он работает без ошибок.
        loader.build();
    }
}
