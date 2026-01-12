package su.erik.tabledataloader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.puls.centralpricing.common.exception.StandardFault;
import su.erik.tabledataloader.dto.LoaderHttpStatus;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TableDataLoaderErrorTest {

    @Test
    @DisplayName("ERROR: Попытка сборки SINGLE_ENTITY без стратегии сохранения")
    void testBuildSingleEntityWithoutSave() {
        var response = TableDataLoader.create()
                .build(TableDataLoader.BuildMode.SINGLE_ENTITY);

        assertEquals(LoaderHttpStatus.NOT_FOUND, response.getStatus());
    }

    @Test
    @DisplayName("ERROR: Импорт без указания файла в параметрах")
    void testImportWithoutFile() {
        assertThrows(StandardFault.class, () -> {
            @SuppressWarnings("unchecked")
            Class<Map<String, Object>> dtoClass = (Class<Map<String, Object>>) (Class<?>) java.util.Map.class;
            TableDataLoader.<Map<String, Object>>create()
                    // Не установили FILE_PARAM
                    .useImportMapper(new TableDataLoaderTest.DummyImportMapper())
                    .build(su.erik.tabledataloader.TableDataLoaderTest.DummyFileImporter.class
                            , dtoClass);
        }, "Должно быть выброшено исключение, если файл не передан");
    }

    @Test
    @DisplayName("ERROR: Ошибка внутри стратегии данных пробрасывается наружу")
    void testExceptionInStrategy() {
        assertThrows(RuntimeException.class, () -> TableDataLoader.create()
                .useToGetData(ignored -> { throw new RuntimeException("DB Error"); })
                .build());
    }
}
