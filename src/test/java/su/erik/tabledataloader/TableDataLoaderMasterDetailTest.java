package su.erik.tabledataloader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import su.erik.tabledataloader.config.Constant;
import su.erik.tabledataloader.dto.DataResponse;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TableDataLoaderMasterDetailTest {

    // Вспомогательный метод для создания Map
    private Map<String, Object> createMap(Object id, Object masterId, String name) {
        Map<String, Object> map = new HashMap<>();
        if (id != null) map.put("id", id);
        if (masterId != null) map.put(Constant.MASTER_ID, masterId);
        map.put("name", name);
        return map;
    }

    @Test
    @DisplayName("Master-Detail: Стандартная вставка детей в EXPANDED_KEY")
    void testStandardMasterDetail() {
        // 1. Мастер данные (2 родителя)
        List<Map<String, Object>> masters = List.of(
                createMap(10, 100, "Parent A"),
                createMap(20, 200, "Parent B") // У этого не будет детей
        );

        // 2. Дети (2 ребенка для Parent A)
        List<Map<String, Object>> children = List.of(
                createMap(1, 100, "Child A1"),
                createMap(2, 100, "Child A2")
        );

        // 3. Запуск
        DataResponse<Map<String, Object>> response = TableDataLoader.<Map<String, Object>>create()
                .useToGetData(p -> masters)
                .useChildList(p -> {
                    // Проверяем, что ID родителей передались в mapParam
                    List<Object> ids = p.getMasterListId();
                    assertNotNull(ids);
                    assertTrue(ids.contains("'100'") || ids.contains(100)); // Зависит от formatMasterId
                    return children;
                })
                .build();

        // 4. Проверки
        List<Map<String, Object>> resultItems = response.getItems();
        assertEquals(2, resultItems.size());

        // Parent A
        Map<String, Object> parentA = resultItems.get(0);
        assertEquals("Parent A", parentA.get("name"));
        assertTrue(parentA.containsKey(Constant.EXPANDED_KEY));

        List<Map<String, Object>> expandedA = (List<Map<String, Object>>) parentA.get(Constant.EXPANDED_KEY);
        assertEquals(2, expandedA.size());
        assertEquals("Child A1", expandedA.get(0).get("name"));

        // Parent B
        Map<String, Object> parentB = resultItems.get(1);
        assertEquals("Parent B", parentB.get("name"));
        // Если детей нет, список пустой
        List<Map<String, Object>> expandedB = (List<Map<String, Object>>) parentB.get(Constant.EXPANDED_KEY);
        assertNotNull(expandedB);
        assertTrue(expandedB.isEmpty());
    }

/*
    @Test
    @DisplayName("Master-Detail: Flattening (Замена родителя ребенком)")
    void testReplaceParentByChild() {
        // Сценарий: Если у родителя ровно 1 ребенок, родитель полностью заменяется полями ребенка.

        // Мастер
        List<Map<String, Object>> masters = List.of(
                createMap(10, 100, "Parent Name") // Это имя должно исчезнуть
        );

        // Ребенок
        List<Map<String, Object>> children = List.of(
                createMap(50, 100, "Child Name") // Это имя должно остаться
        );

        DataResponse<Map<String, Object>> response = TableDataLoader.<Map<String, Object>>create()
                .useToGetData(p -> masters)
                .useChildList(p -> children)
                .replaceParentByChild(Collections.emptyList()) // Включаем режим замены (список полей для удаления пустой, значит полная замена)
                .build();

        Map<String, Object> resultItem = response.getItems().getFirst();

        // Проверяем, что в результирующей мапе данные от ребенка
        assertEquals(50, resultItem.get("id"));
        assertEquals("Child Name", resultItem.get("name"));
        // Ключа EXPANDED_KEY быть не должно, так как произошло слияние
        assertFalse(resultItem.containsKey(Constant.EXPANDED_KEY));
    }
*/

    @Test
    @DisplayName("Master-Detail: Flattening с удалением полей (когда детей > 1)")
    void testReplaceParentByChildMulti() {
        // Сценарий: Если детей много, мы просто добавляем их в expanded,
        // НО удаляем из детей указанные поля (например, лишний мусор или ID родителя).

        List<Map<String, Object>> masters = List.of(createMap(10, 100, "Parent"));

        List<Map<String, Object>> children = List.of(
                createMap(1, 100, "C1"),
                createMap(2, 100, "C2")
        );
        // Добавим "мусорное" поле ребенку
        children.get(0).put("tempField", "trash");

        DataResponse<Map<String, Object>> response = TableDataLoader.<Map<String, Object>>create()
                .useToGetData(p -> masters)
                .useChildList(p -> children)
                .replaceParentByChild(List.of("tempField", Constant.MASTER_ID)) // Удалить эти поля из детей
                .build();

        Map<String, Object> parent = response.getItems().getFirst();
        List<Map<String, Object>> expanded = (List<Map<String, Object>>) parent.get(Constant.EXPANDED_KEY);

        assertEquals(2, expanded.size());

        // Проверяем очистку
        Map<String, Object> child1 = expanded.get(0);
        assertEquals("C1", child1.get("name"));
        assertFalse(child1.containsKey("tempField"), "Поле tempField должно быть удалено");
        assertFalse(child1.containsKey(Constant.MASTER_ID), "Поле MASTER_ID должно быть удалено");
    }

    @Test
    @DisplayName("Master-Detail: Игнорируется, если данные не Map")
    void testIgnoredForNonMap() {
        // Если T не Map, а например String POJO, логика processMasterDetail пропускается
        DataResponse<String> response = TableDataLoader.<String>create()
                .useToGetData(p -> List.of("String Data"))
                .useChildList(p -> Collections.emptyList()) // Стратегия задана
                .build();

        // Ошибки нет, просто вернулся список строк
        assertEquals("String Data", response.getItems().getFirst());
    }
}
