package su.erik.tabledataloader.importer.csv;

import org.junit.jupiter.api.Test;
import su.erik.tabledataloader.TableDataLoader;
import su.erik.tabledataloader.DynamicImportTestDTO;
import su.erik.tabledataloader.importer.model.ResultDTO;
import su.erik.tabledataloader.dto.DataResponse;
import su.erik.tabledataloader.dto.InputFile;
import su.erik.tabledataloader.importer.factory.ReflectionFileImporterFactory;
import su.erik.tabledataloader.context.DataLoaderContext;
import su.erik.tabledataloader.LoaderRegistry;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RealMyBatisImportTest {

    @Test
    void testRealImportCycle() {
        // Создаем изолированный контекст для теста
        LoaderRegistry registry = new LoaderRegistry();
        registry.setLoaderFactory(new ReflectionFileImporterFactory());
        DataLoaderContext context = new DataLoaderContext(registry, null);
        
        // CSV: ID;Code;Name;Region1;Region2
        String csvData = "ID;Code;Name;Region1;Region2\n100;C100;Item100;10.5;20.5\n";
        InputFile inputFile = new InputFile() {
            @Override public InputStream getInputStream() { return new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8)); }
            @Override public String getOriginalFilename() { return "test.csv"; }
            @Override public long getSize() { return csvData.length(); }
        };

        MockMapper<DynamicImportTestDTO> importMapper = new MockMapper<>();

        var loader = TableDataLoader.<DynamicImportTestDTO>create(context)
                .setMapParam(su.erik.tabledataloader.config.Constant.FILE_PARAM, inputFile)
                .useImportMapper(importMapper);

        DataResponse<ResultDTO> importResponse = loader.build(DynamicImportTestDTO.class);
        
        // Используем items() т.к. DataResponse это record
        assertFalse(importResponse.items().isEmpty(), "Список результатов импорта не должен быть пустым");
        ResultDTO result = importResponse.items().get(0);
        
        assertEquals(1, result.count(), "Количество импортированных строк должно быть 1");
        assertEquals(1, importMapper.getImportedItems().size(), "Маппер должен содержать 1 объект");
        
        DynamicImportTestDTO importedDto = importMapper.getImportedItems().get(0);
        assertEquals(100L, importedDto.getId(), "ID должен быть 100");
        assertEquals("C100", importedDto.getCode(), "Код должен быть C100");
    }
}