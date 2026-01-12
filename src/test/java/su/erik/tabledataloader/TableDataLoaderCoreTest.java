package su.erik.tabledataloader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import su.erik.tabledataloader.dto.DataResponse;
import su.erik.tabledataloader.dto.LoaderHttpStatus;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class TableDataLoaderCoreTest {

    // --- 1. GET (Select List) ---

    @Test
    @DisplayName("GET: Получение списка данных и проверка параметров")
    void testGetList() {
        // Данные для теста
        List<String> mockData = List.of("Item A", "Item B");

        // Создаем загрузчик
        DataResponse<String> response = TableDataLoader.<String>create()
                .setMapParam("status", "ACTIVE") // Устанавливаем фильтр
                .setLimit(50)
                .useToGetData(param -> {
                    // Проверяем, что параметры дошли до стратегии
                    assertEquals("ACTIVE", param.getFilters().get("status"));
                    assertEquals(50, param.getLimit());
                    return mockData;
                })
                .build();

        // Проверяем ответ
        assertEquals(LoaderHttpStatus.OK, response.getStatus());
        assertEquals(2, response.getItems().size());
        assertEquals("Item A", response.getItems().getFirst());
        // Если countFetcher не задан, total = size
        assertEquals(2L, response.getTotal());
    }

    @Test
    @DisplayName("GET: Проверка пагинации (CountFetcher)")
    void testGetListWithCount() {
        DataResponse<Integer> response = TableDataLoader.<Integer>create()
                .useToGetData(p -> List.of(1, 2, 3)) // Вернули 3 элемента (страница)
                .useToCount(p -> 100L)                // Всего в базе 100 элементов
                .build();

        assertEquals(3, response.getItems().size());
        assertEquals(100L, response.getTotal());
    }

    @Test
    @DisplayName("GET: Consumer (Пост-обработка)")
    void testForEachConsumer() {
        AtomicBoolean processed = new AtomicBoolean(false);

        TableDataLoader.<String>create()
                .useToGetData(p -> List.of("Raw"))
                .setForEachConsumer(item -> {
                    assertEquals("Raw", item);
                    processed.set(true);
                })
                .build();

        assertTrue(processed.get(), "Consumer должен был быть вызван");
    }

    // --- 2. SAVE (POST/PUT) ---

    @Test
    @DisplayName("SAVE: Создание сущности (BuildMode.SINGLE_ENTITY)")
    void testSaveSingleEntity() {
        DataResponse<String> response = TableDataLoader.<String>create()
                .setStatus(LoaderHttpStatus.CREATED)
                .setMapParam("name", "New Item")
                .useToSave(param -> {
                    assertEquals("New Item", param.getFilters().get("name"));
                    return "Created ID:1";
                })
                .build(TableDataLoader.BuildMode.SINGLE_ENTITY);

        assertEquals(LoaderHttpStatus.CREATED, response.getStatus());
        assertEquals(1, response.getItems().size());
        assertEquals("Created ID:1", response.getItems().getFirst());
        assertEquals(1L, response.getTotal());
    }

    @Test
    @DisplayName("SAVE: Ошибка 404 если стратегия не задана")
    void testSaveWithoutStrategy() {
        DataResponse<String> response = TableDataLoader.<String>create()
                .build(TableDataLoader.BuildMode.SINGLE_ENTITY);

        assertEquals(LoaderHttpStatus.NOT_FOUND, response.getStatus());
    }

    // --- 3. DELETE (Exec) ---

    @Test
    @DisplayName("DELETE: Выполнение действия с возвратом количества")
    void testDelete() {
        DataResponse<Long> response = TableDataLoader.<Void>create()
                .setMapParam("id", 123)
                .useImportMapper(param -> {
                    assertEquals(123, param.getFilters().get("id"));
                    return 5L; // Удалено 5 записей
                })
                .buildDelete();

        assertEquals(LoaderHttpStatus.OK, response.getStatus());
        assertEquals(5L, response.getItems().getFirst());
        assertEquals(5L, response.getTotal());
    }
}
