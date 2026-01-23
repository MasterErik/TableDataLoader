package su.erik.tabledataloader.importer.csv;

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
import su.erik.tabledataloader.DynamicImportTestDTO;
import su.erik.tabledataloader.DynamicImportTestMapper;
import su.erik.tabledataloader.TableDataLoader;
import su.erik.tabledataloader.config.StandardParam;
import su.erik.tabledataloader.dto.DataResponse;
import su.erik.tabledataloader.dto.InputFile;
import su.erik.tabledataloader.importer.dto.ImportResultDTO;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RealMyBatisImportTest {

    private static SqlSessionFactory sqlSessionFactory;

    // --- 3. Input File ---
    static class StringInputFile implements InputFile {
        private final byte[] content;
        private final String name;
        public StringInputFile(String name, String content) {
            this.name = name;
            this.content = content.getBytes(StandardCharsets.UTF_8);
        }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(content); }
        @Override public String getOriginalFilename() { return name; }
        @Override public long getSize() { return content.length; }
    }

    @BeforeAll
    static void setup() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:dynamic_import_test;DB_CLOSE_DELAY=-1;MODE=Oracle;DATABASE_TO_UPPER=FALSE");
        ds.setUser("sa");

        // Init Schema
        try (Connection conn = ds.getConnection(); Statement stmt = conn.createStatement()) {
            String schema = new String(Resources.getResourceAsStream("su/erik/tabledataloader/schema-dynamic-import.sql").readAllBytes());
            stmt.execute(schema);
        }

        Environment environment = new Environment("test", new JdbcTransactionFactory(), ds);
        Configuration configuration = new Configuration(environment);
        configuration.setLogImpl(StdOutImpl.class);

        // Register Mapper
        configuration.addMapper(DynamicImportTestMapper.class);

        // Load XMLs
        loadXml(configuration, "su/erik/tabledataloader/mybatis/Common.xml");
        loadXml(configuration, "su/erik/tabledataloader/DynamicImportTestMapper.xml");

        sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
    }

    private static void loadXml(Configuration configuration, String resource) throws Exception {
        try (InputStream is = Resources.getResourceAsStream(resource)) {
            if (is == null) throw new RuntimeException("Resource not found: " + resource);
            XMLMapperBuilder builder = new XMLMapperBuilder(is, configuration, resource, configuration.getSqlFragments());
            builder.parse();
        }
    }

    @Test
    @DisplayName("MyBatis + TableDataLoader: Полный цикл импорта динамических колонок в H2")
    void testRealImportCycle() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            DynamicImportTestMapper mapper = session.getMapper(DynamicImportTestMapper.class);

            // 1. Подготовка данных
            // Code; Name; Jan; Feb; Mar
            String csvData = "StaticCode;StaticName;Jan;Feb;Mar\n" +
                             "100;Item A;10.5;20.5;30.5\n" +
                             "200;Item B;15.0;25.0;35.0";

            InputFile file = new StringInputFile("data.csv", csvData);
            String tempTable = "temp_dynamic_import";
            String targetTable = "dynamic_import_result";

            // 2. Настройка загрузчика
            Map<String, Object> filters = new HashMap<>();
            filters.put("tempTableName", tempTable); // Для <insert>
            filters.put("targetTableName", targetTable); // Для <finish> (финальная таблица)

            DataResponse<ImportResultDTO> response = TableDataLoader.<DynamicImportTestDTO>create()
                    .setMapParam(StandardParam.FILE.getKey(), file)
                    .setMapParam(StandardParam.ENTITY.getKey(), "DynamicEntity")
                    .setMapParam(StandardParam.USER_ID.getKey(), 777L)
                    .setMapParam("tempTableName", tempTable)
                    .setMapParam("targetTableName", targetTable) // Передаем
                    .useImportMapper(mapper)
                    .build(CsvFileImporter.class, DynamicImportTestDTO.class);

            // 3. Проверки результата
            assertEquals(1, response.getItems().size());
            ImportResultDTO resultDTO = response.getItems().get(0);
            assertEquals(2, resultDTO.count()); // 2 строки в CSV
            long uploadId = resultDTO.uploadId();

            // 4. Проверка развернутых данных в H2 (UNPIVOT результат)
            // Было 2 строки и 3 динамические колонки (Jan, Feb, Mar) -> должно быть 6 строк в dynamic_import_result
            List<Map<String, Object>> resultRows = mapper.selectResult(uploadId, targetTable);
            assertEquals(6, resultRows.size(), "Должно быть 6 строк после разворота столбцов (2 записи * 3 месяца)");

            // Проверим конкретное значение (например, Jan для первой записи)
            Map<String, Object> janA = resultRows.stream()
                    .filter(r -> ((Number)r.get("static_code")).longValue() == 100L && "Jan".equals(r.get("column_name")))
                    .findFirst().orElseThrow();
            assertEquals(10.5, ((Number) janA.get("column_value")).doubleValue());

            Map<String, Object> marB = resultRows.stream()
                    .filter(r -> ((Number)r.get("static_code")).longValue() == 200L && "Mar".equals(r.get("column_name")))
                    .findFirst().orElseThrow();
            assertEquals(35.0, ((Number) marB.get("column_value")).doubleValue());

            session.commit();
        }
    }
}
