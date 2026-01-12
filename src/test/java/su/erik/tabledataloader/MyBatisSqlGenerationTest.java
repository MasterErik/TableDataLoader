package su.erik.tabledataloader;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.logging.stdout.StdOutImpl;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import su.erik.tabledataloader.config.Constant;
import su.erik.tabledataloader.param.Filter;
import su.erik.tabledataloader.param.MapParam;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MyBatisSqlGenerationTest {

    private static SqlSessionFactory sqlSessionFactory;

    @BeforeAll
    static void setupAll() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=Oracle;DATABASE_TO_UPPER=FALSE");
        ds.setUser("sa");

        Environment environment = new Environment("test", new JdbcTransactionFactory(), ds);
        Configuration configuration = new Configuration(environment);
        configuration.setLogImpl(StdOutImpl.class);

        loadXml(configuration, "com/puls/centralpricing/handlers/mapper/reuse/Common.xml");
        loadXml(configuration, "su/erik/tabledataloader/TestMapper.xml");

        sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
    }

    private static void loadXml(Configuration configuration, String resource) throws Exception {
        try (InputStream is = Resources.getResourceAsStream(resource)) {
            if (is == null) throw new RuntimeException("Resource not found in classpath: " + resource);
            XMLMapperBuilder builder = new XMLMapperBuilder(is, configuration, resource, configuration.getSqlFragments());
            builder.parse();
        }
    }

    @Test
    @DisplayName("Logic: Тест пагинации на сгенерированных данных Paging (OFFSET/LIMIT)")
    void testDataPagination() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            TestMapper mapper = session.getMapper(TestMapper.class);
            MapParam param = new MapParam();

            // 1. Параметры генерации: создаем 50 записей (1..50)
            param.filter("number", 50);

            // 2. Параметры пагинации:
            int offset = 5;
            int limit = 10;
            param.setOffset(offset);
            param.setLimit(limit);

            List<Map<String, Object>> result = mapper.testSelect(param);

            assertNotNull(result);
            assertEquals(limit, result.size(), "Должно вернуться ровно " + limit + " записей");

            Number firstId = (Number) result.getFirst().get("id");
            assertEquals(offset + 1, firstId.intValue(), "Первый ID должен быть " + (offset + 1));

            Number lastId = (Number) result.getLast().get("id");
            assertEquals(offset + limit, lastId.intValue(), "Последний ID должен быть " + (offset + limit));
        }
    }

    @Test
    @DisplayName("MyBatis: Генерация ORDER BY")
    void testOrderingSql() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            TestMapper mapper = session.getMapper(TestMapper.class);
            MapParam param = new MapParam();
            param.filter("number", 10); // Generate data

            param.addOrderBy("1", "DESC");

            List<Map<String, Object>> result = mapper.testSelect(param);
            assertNotNull(result);
        }
    }

    @Test
    @DisplayName("MyBatis: Проверка одиночных фильтров для базовых типов")
    void testCriteriaMapping() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            TestMapper mapper = session.getMapper(TestMapper.class);
            MapParam param = new MapParam()
                .filter("number", 10) // Generate 10 records (1..10)
                .addCriteria("id", ">", 2) // 3,4,5,6,7,8,9,10
                .addCriteria("is_active", true); // Even numbers: 4, 6, 8, 10

            List<Map<String, Object>> result = mapper.testSelect(param);
            assertEquals(4, result.size(), "Should find 4 records (4, 6, 8, 10)");
        }
    }

    @Test
    @DisplayName("MyBatis: Генерация Keyword Search (поиск по колонкам)")
    void testKeywordSearchSql() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            TestMapper mapper = session.getMapper(TestMapper.class);
            MapParam param = new MapParam();
            param.filter("number", 10); // Generate data

            param.setKeywordSearch("test%");
            param.setColumns("name");

            List<Map<String, Object>> result = mapper.testSelect(param);
            assertNotNull(result);
            assertFalse(result.isEmpty());
        }
    }

    @Test
    @DisplayName("MyBatis: Генерация Master List ID (IN clause)")
    void testMasterListSql() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            TestMapper mapper = session.getMapper(TestMapper.class);
            MapParam param = new MapParam();
            param.filter("number", 20); // Generate 20 records

            param.setMasterListId(List.of(1, 2));

            List<Map<String, Object>> result = mapper.testSelect(param);
            assertNotNull(result);
            assertEquals(2, result.size());
        }
    }

    @Test
    @DisplayName("MyBatis: Проверка маппинга фильтра IN (CriteriaIn)")
    void testInFilterMapping() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            TestMapper mapper = session.getMapper(TestMapper.class);
            MapParam param = new MapParam()
                .filter("number", 10)
                .addCriteria("id", List.of(1, 2, 3));

            List<Map<String, Object>> result = mapper.testSelect(param);
            assertFalse(result.isEmpty());
            assertEquals(3, result.size());
        }
    }

    @Test
    @DisplayName("MyBatis: Проверка маппинга BETWEEN (value + valueR)")
    void testBetweenMapping() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            TestMapper mapper = session.getMapper(TestMapper.class);
            MapParam param = new MapParam()
                .filter("number", 20)
                .addCriteria("id", Constant.BETWEEN, 5, 15);

            List<Map<String, Object>> result = mapper.testSelect(param);
            assertFalse(result.isEmpty());
        }
    }

    @Test
    @DisplayName("MyBatis: Проверка группировки условий через скобки")
    void testBracketsMapping() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            TestMapper mapper = session.getMapper(TestMapper.class);

            MapParam param = new MapParam()
                    .filter("number", 10)
                    .addCriteria("name", "test №1")
                    .addCriteria("id", "=", 2, Filter.SqlSuffix.OR) // OR ...
                    .openBracket() // (
                        .addCriteria("name", "test №2")
                    .closeBracket(); // )

            // Result should contain id=1 and id=2.
            List<Map<String, Object>> result = mapper.testSelect(param);
            assertFalse(result.isEmpty());
        }
    }

    @Test
    @DisplayName("MyBatis: Стресс-тест - сложная вложенность")
    void testComplexBracketsAndTypes() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            TestMapper mapper = session.getMapper(TestMapper.class);

            MapParam param = new MapParam();
            param.filter("number", 20); // Generate 20 records

            param.openBracket() // Global Open
                // Group 1
                .openBracket()
                .addCriteria("1", "=", 1)
                .addCriteria("name", "LIKE", "test%", Filter.SqlSuffix.OR) // Connects to next group with OR
                .closeBracket()

                // Group 2
                .openBracket()
                .addCriteria("id", ">", 50)
                .addCriteria("id", List.of(1, 2, 3))
                .closeBracket()

                .closeBracket(); // Global Close

            param.addCriteria("1", "=", 1);
            param.addCriteria("id", Constant.BETWEEN, 5, 15);

            param.addOrderBy("id", "ASC");

            List<Map<String, Object>> result = mapper.testSelect(param);

            assertEquals(11, result.size(), "Should return IDs 5 through 15 (11 records)");

            assertEquals(5, ((Number) result.getFirst().get("id")).intValue());
            assertEquals(15, ((Number) result.getLast().get("id")).intValue());
        }
    }
}
