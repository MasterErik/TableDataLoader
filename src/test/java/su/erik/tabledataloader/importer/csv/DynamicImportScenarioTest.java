package su.erik.tabledataloader.importer.csv;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import su.erik.tabledataloader.TableDataLoader;
import su.erik.tabledataloader.config.Constant;
import su.erik.tabledataloader.config.StandardParam;
import su.erik.tabledataloader.dto.DataResponse;
import su.erik.tabledataloader.dto.InputFile;
import su.erik.tabledataloader.dto.LoaderHttpStatus;
import su.erik.tabledataloader.importer.ImportMapper;
import su.erik.tabledataloader.importer.annotation.DynamicColumn;
import su.erik.tabledataloader.importer.annotation.ImporterBindByIndex;
import su.erik.tabledataloader.importer.dto.ImportResultDTO;
import su.erik.tabledataloader.importer.dto.dynamic.CsvImportDynamicDTO;
import su.erik.tabledataloader.importer.dto.UploadDTO;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Серия тестов, демонстрирующая сценарий импорта с динамическими колонками
 * (как в BalanceAssortmentImportMapper).
 */
class DynamicImportScenarioTest {

    // --- 1. DTO ---
    @DynamicColumn(startsFromIndex = 2)
    public static class BalanceAssortmentImportDTO extends CsvImportDynamicDTO<Double> {
        private Long goodCode;
        private String goodName;

        public BalanceAssortmentImportDTO(Integer dynamicValueSize) {
            super(dynamicValueSize);
        }

        @ImporterBindByIndex(0)
        public void setGoodCode(String value) {
            try {
                this.goodCode = Double.valueOf(value).longValue();
            } catch (NumberFormatException e) {
                this.goodCode = null;
            }
        }

        @ImporterBindByIndex(1)
        public void setGoodName(String value) {
            this.goodName = value;
        }

        @Override
        public void setterDynamicValue(String value, Integer index) {
            if (value != null && !value.isBlank()) {
                this.dynamicValues.set(index, Double.valueOf(value));
            }
        }

        public Long getGoodCode() { return goodCode; }
        public String getGoodName() { return goodName; }
    }

    // --- 2. Mock Mapper ---
    static class MockBalanceMapper implements ImportMapper<BalanceAssortmentImportDTO> {
        final List<BalanceAssortmentImportDTO> capturedItems = new ArrayList<>();

        @Override
        public void insertHeader(UploadDTO uploadDTO) {
            uploadDTO.setId(100L);
        }

        @Override
        public void insert(Map<String, Object> customFilters) {
            // Эмуляция MyBatis <insert>:
            // MyBatis берет объект из map и генерирует SQL
            BalanceAssortmentImportDTO dto = (BalanceAssortmentImportDTO) customFilters.get("importDTO");
            
            // Здесь мы просто сохраняем его для проверки
            capturedItems.add(dto);
            
            // Эмуляция SQL генерации (проверка, что список доступен)
            StringBuilder sql = new StringBuilder("INSERT INTO ##temp VALUES (");
            sql.append(dto.getGoodCode()).append(", ");
            sql.append("'").append(dto.getGoodName()).append("', ");
            // <foreach collection="dynamicValues">
            for (Double val : dto.dynamicValues) {
                sql.append(val).append(", ");
            }
            sql.setLength(sql.length() - 2); // remove last comma
            sql.append(")");
            
            // System.out.println("Generated SQL: " + sql);
        }

        @Override public void createTempTable(List<String> headers, String tempTableName) {}
        @Override public void delete(long uploadId) {}
        @Override public void finish(Map<String, Object> customFilters) {}
        @Override public void flush() {}
    }

    // --- 3. Mock Input File ---
    static class TestInputFile implements InputFile {
        private final byte[] content;
        private final String name;

        public TestInputFile(String name, String csvContent) {
            this.name = name;
            this.content = csvContent.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(content);
        }

        @Override
        public String getOriginalFilename() {
            return name;
        }

        @Override
        public long getSize() {
            return content.length;
        }
    }

    // =================================================================================================================
    //                                         TESTS
    // =================================================================================================================

    @Nested
    @DisplayName("Stage 1: Проверка DTO и логики заполнения")
    class Stage1_DtoLogic {
        @Test
        void testDtoPopulation() {
            // Ручное создание и заполнение (как делает парсер)
            BalanceAssortmentImportDTO dto = new BalanceAssortmentImportDTO(3); // 3 динамические колонки
            
            // Static
            dto.setGoodCode("101.0");
            dto.setGoodName("Item 1");
            
            // Dynamic
            dto.setterDynamicValue("10.5", 0);
            dto.setterDynamicValue("20.0", 1);
            dto.setterDynamicValue("30.0", 2);

            // Assertions
            assertEquals(101L, dto.getGoodCode());
            assertEquals("Item 1", dto.getGoodName());
            assertEquals(3, dto.dynamicValues.size());
            assertEquals(10.5, dto.dynamicValues.get(0));
            assertEquals(30.0, dto.dynamicValues.get(2));
        }
    }

    @Nested
    @DisplayName("Stage 2: Прямой вызов Importer")
    class Stage2_DirectImport {
        @Test
        void testDirectImport() {
            // GoodCode; Name;   Dyn1;  Dyn2
            String csv = "HeaderA;HeaderB;Val1;Val2\n" +
                         "202;Item 2;55.5;66.6";
            
            MockBalanceMapper mapper = new MockBalanceMapper();
            Map<String, Object> filters = new HashMap<>();
            filters.put(Constant.SKIP_LINES, 1); // Пропускаем заголовок

            CsvFileImporter<BalanceAssortmentImportDTO> importer = 
                    new CsvFileImporter<>(BalanceAssortmentImportDTO.class, mapper, filters);

            ByteArrayInputStream is = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
            ImportResultDTO result = importer.importFile(is, "test.csv", csv.length(), "entity", 1L);

            assertEquals(1, result.count());
            assertEquals(1, mapper.capturedItems.size());
            
            BalanceAssortmentImportDTO dto = mapper.capturedItems.get(0);
            assertEquals(202L, dto.getGoodCode());
            assertEquals(2, dto.dynamicValues.size());
            assertEquals(55.5, dto.dynamicValues.get(0));
        }
    }

    @Nested
    @DisplayName("Stage 3: Полный цикл через TableDataLoader")
    class Stage3_TableDataLoader {
        @Test
        void testTableDataLoaderImport() {
            // CSV Data
            String csvData = "Code;Name;Reg1;Reg2;Reg3\n" +
                             "303;Item 3;1.1;2.2;3.3\n" +
                             "404;Item 4;4.4;5.5;6.6";
            
            TestInputFile file = new TestInputFile("import.csv", csvData);
            MockBalanceMapper mapper = new MockBalanceMapper();

            // Setup Loader
            // Generic type <BalanceAssortmentImportDTO> is inferred
            DataResponse<ImportResultDTO> response = TableDataLoader.<BalanceAssortmentImportDTO>create()
                    .setMapParam(StandardParam.FILE.getKey(), file)
                    .setMapParam(StandardParam.ENTITY.getKey(), "Balance")
                    .setMapParam(StandardParam.USER_ID.getKey(), 999L)
                    // Важно: передаем skipLines через кастомный фильтр
                    .setMapParam(Constant.SKIP_LINES, 1)
                    .useImportMapper(mapper)
                    .build(CsvFileImporter.class, BalanceAssortmentImportDTO.class);

            // Checks
            assertEquals(LoaderHttpStatus.OK, response.getStatus());
            assertEquals(1, response.getItems().size()); // 1 ImportResultDTO
            assertEquals(2, response.getItems().get(0).count()); // 2 rows imported
            
            // Mapper verification
            assertEquals(2, mapper.capturedItems.size());
            
            BalanceAssortmentImportDTO item1 = mapper.capturedItems.get(0);
            assertEquals(303L, item1.getGoodCode());
            assertEquals(3, item1.dynamicValues.size());
            assertEquals(1.1, item1.dynamicValues.get(0));
            
            BalanceAssortmentImportDTO item2 = mapper.capturedItems.get(1);
            assertEquals(404L, item2.getGoodCode());
            assertEquals(6.6, item2.dynamicValues.get(2));
        }
    }
}
