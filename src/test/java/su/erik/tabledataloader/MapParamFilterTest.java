package su.erik.tabledataloader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import su.erik.tabledataloader.config.Constant;
import su.erik.tabledataloader.param.Filter;
import su.erik.tabledataloader.param.Filter.SqlSuffix;
import su.erik.tabledataloader.param.MapParam;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MapParamFilterTest {

    @Test
    @DisplayName("Filter: Модификаторы LIKE/ILIKE (автоматические %)")
    void testLikeModifiers() {
        MapParam param = new MapParam();

        // 1. LIKE -> %val%
        param.addCriteria("name", Constant.LIKE, "John");
        // 2. ILIKE -> %val% (то же самое, но регистронезависимо в SQL)
        param.addCriteria("email", Constant.ILIKE, "gmail");
        // 3. END_LIKE -> val%
        param.addCriteria("phone", Constant.END_LIKE, "999");

        List<Filter> list = param.getCriteria();
        assertEquals(3, list.size());

        // Проверяем, что проценты добавились
        assertEquals("%John%", list.get(0).getValue());
        assertEquals("%gmail%", list.get(1).getValue());
        assertEquals("999%", list.get(2).getValue());
    }

    @Test
    @DisplayName("Paging & Sort: Проверка наследования и состояния")
    void testPagingAndSortingState() {
        MapParam param = new MapParam();

        // Установка пагинации
        param.setLimit(10);
        param.setOffset(5);

        // Установка сортировки
        param.addOrderBy("created_at", "DESC");
        param.addOrderBy("name", "ASC");

        // Проверка полей (геттеры из Paging/Sort)
        assertEquals(10, param.getLimit());
        assertEquals(5, param.getOffset());

        assertFalse(param.getOrderBy().isEmpty()); // isExists() проверяет наличие сортировки
        assertEquals(2, param.getOrderBy().size());

        assertEquals("created_at", param.getOrderBy().getFirst().getSortBy());
        assertEquals(MapParam.SortDirection.DESC, param.getOrderBy().getFirst().getSortOrder());
    }

    @Test
    @DisplayName("Filter: Диапазоны (BETWEEN)")
    void testBetween() {
        // Используем 4-аргументный метод (fieldName, op, val, valR)
        // В Kotlin он теперь проксирует вызов в главный метод
        MapParam param = new MapParam()
                .addCriteria("age", Constant.BETWEEN, 18, 30);

        List<Filter> list = param.getCriteria();
        assertEquals(1, list.size());

        Filter filter = list.getFirst();
        assertEquals(Constant.BETWEEN, filter.getOp());
        assertEquals(18, filter.getValue());
        assertEquals(30, filter.getValueR()); // Проверяем второе значение
    }

    @Test
    @DisplayName("Filter: Проверка автоматических суффиксов в цепочке")
    void testSuffixAutomation() {
        MapParam param = new MapParam()
                .addCriteria("f1", "v1")
                .addCriteria("f2", "v2")
                .addCriteria("f3", "v3");

        List<Filter> list = param.getCriteria();
        assertEquals(3, list.size());

        // AND проставляется автоматически ПРЕДЫДУЩЕМУ элементу при добавлении нового
        assertEquals(SqlSuffix.AND.name(), list.get(0).getSqlSuffix());
        assertEquals(SqlSuffix.AND.name(), list.get(1).getSqlSuffix());

        // У последнего элемента суффикс пустой (CLOSE)
        assertEquals("", list.get(2).getSqlSuffix());
        assertEquals(SqlSuffix.CLOSE, list.get(2).getSuffix());
    }

    @Test
    @DisplayName("Filter: Обработка null значений")
    void testNullHandling() {
        MapParam param = new MapParam();
        // Метод addCriteria должен игнорировать null value
        param.addCriteria("hidden", null);
        assertTrue(param.getCriteria().isEmpty(), "Criteria should be empty when nulls are passed");
    }

    @Test
    @DisplayName("Filter: Проверка состояния скобок (Unit)")
    void testComplexBracketsState() {
        MapParam param = new MapParam();

        param.openBracket()
                .addCriteria("ID", "=", 1, SqlSuffix.OR)
                .openBracket()
                .addCriteria("NAME", "test")
                .addCriteria("IS_ACTIVE", true)
                .closeBracket()
                .closeBracket();

        List<Filter> list = param.getCriteria();
        assertEquals(3, list.size());

        // 1. ID=1
        Filter fId = list.getFirst();
        assertEquals("(", fId.getLBracket());
        assertEquals(SqlSuffix.OR, fId.getSuffix());

        // 2. NAME='test'
        Filter fName = list.get(1);
        assertEquals("(", fName.getLBracket());
        assertEquals(SqlSuffix.AND.name(), fName.getSqlSuffix());

        // 3. IS_ACTIVE=true
        Filter fActive = list.get(2);
        assertEquals("))", fActive.getRBracket());
        assertEquals(SqlSuffix.CLOSE, fActive.getSuffix());
    }

    @Test
    @DisplayName("Filter: Метод filter() пишет в отдельную карту filters")
    void testFilterMap() {
        MapParam param = new MapParam();

        param.filter("tableParam", "myTable");
        param.setTable("users");

        // criteria list должен быть пуст, так как filter() пишет в HashMap
        assertTrue(param.getCriteria().isEmpty());

        assertEquals("myTable", param.getFilters().get("tableParam"));
        assertEquals("users", param.getFilters().get(Constant.TABLE_NAME));
    }
}
