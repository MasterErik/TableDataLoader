package su.erik.tabledataloader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import su.erik.tabledataloader.dto.LoaderHttpStatus;
import su.erik.tabledataloader.importer.ImportMapper;
import su.erik.tabledataloader.importer.model.UploadDTO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TableDataLoaderCoreTest {

    @Test
    @DisplayName("GET: Consumer (Пост-обработка)")
    void testForEachConsumer() {
        var loader = TableDataLoader.<Map<String, Object>>create()
                .useToGetData(param -> List.of(new HashMap<>()))
                .setForEachConsumer(map -> map.put("processed", true))
                .useImportMapper(new ImportMapper<>() {
                    @Override public void insertHeader(UploadDTO uploadDTO) {}
                    @Override public void insert(Map<String, Object> customFilters) {}
                    @Override public void createTempTable(List<String> headers, String tableName) {}
                    @Override public void delete(long id) {}
                    @Override public void finish(Map<String, Object> customFilters) {}
                    @Override public void flush() {}
                });

        var response = loader.build();
        assertTrue((Boolean) response.items().get(0).get("processed"));
    }

    @Test
    @DisplayName("GET: Получение списка данных и проверка параметров")
    void testGetList() {
        var loader = TableDataLoader.<String>create()
                .useToGetData(param -> List.of("A", "B"))
                .setMapParam("test", "value");

        var response = loader.build();
        assertEquals(2, response.items().size());
        assertEquals("A", response.items().get(0));
    }

    @Test
    @DisplayName("GET: Проверка пагинации (CountFetcher)")
    void testCountFetcher() {
        var loader = TableDataLoader.<String>create()
                .useToGetData(param -> List.of("A"))
                .useToCount(param -> 100L);

        var response = loader.build();
        assertEquals(1, response.items().size());
        assertEquals(100L, response.total());
    }

    @Test
    @DisplayName("SAVE: Создание сущности (BuildMode.SINGLE_ENTITY)")
    void testSaveSingle() {
        var loader = TableDataLoader.<String>create()
                .useToSave(param -> "Saved");

        var response = loader.build(TableDataLoader.BuildMode.SINGLE_ENTITY);
        assertEquals("Saved", response.items().get(0));
    }

    @Test
    @DisplayName("DELETE: Выполнение действия с возвратом количества")
    void testDelete() {
        var loader = TableDataLoader.<Long>create()
                .useToExec(param -> 5L);

        var response = loader.buildDelete();
        assertEquals(5L, response.items().get(0));
    }

    @Test
    @DisplayName("SAVE: Ошибка 404 если стратегия не задана")
    void testSaveNotFound() {
        var loader = TableDataLoader.<String>create();
        var response = loader.build(TableDataLoader.BuildMode.SINGLE_ENTITY);
        assertEquals(LoaderHttpStatus.NOT_FOUND, response.status());
    }
}
