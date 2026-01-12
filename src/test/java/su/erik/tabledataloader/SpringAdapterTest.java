package su.erik.tabledataloader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import su.erik.tabledataloader.config.Constant;
import su.erik.tabledataloader.dto.DataResponse;
import su.erik.tabledataloader.dto.ExportResource;
import su.erik.tabledataloader.dto.LoaderHttpStatus;
import su.erik.tabledataloader.utils.SpringResponseAdapter;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тест проверяет работу адаптера (SpringResponseAdapter),
 * который преобразует независимые DTO библиотеки в Spring ResponseEntity.
 */
class SpringAdapterTest {

    @Test
    @DisplayName("ADAPTER: Конвертация списка (GET) с заголовками")
    void testToListResponseEntity() {
        // 1. Исходный ответ библиотеки (Wrapper)
        DataResponse<String> libResponse = new DataResponse<>(
                List.of("Item A", "Item B"),
                100L,
                Collections.emptyMap(),
                LoaderHttpStatus.OK
        );

        // 2. Конвертация
        // Используем var, чтобы Java сама вывела тип: ResponseEntity<DataResponse<String>>
        var response = SpringResponseAdapter.toListResponseEntity(libResponse);

        // 3. Проверка Spring объекта
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // В теле ответа лежит DataResponse, а не List
        assertNotNull(response.getBody());
        assertEquals(100L, response.getBody().getTotal());
        assertEquals(2, response.getBody().getItems().size());
        assertEquals("Item A", response.getBody().getItems().getFirst());
    }

    @Test
    @DisplayName("ADAPTER: Конвертация одиночной записи (POST/PUT) со статусом CREATED")
    void testToSingleResponseEntity() {
        // 1. Исходный ответ (список из 1 элемента)
        DataResponse<String> libResponse = new DataResponse<>(
                List.of("Created Item"),
                1L,
                Collections.emptyMap(),
                LoaderHttpStatus.CREATED // Статус 201
        );

        // 2. Конвертация (возвращает ResponseEntity<String>)
        var response = SpringResponseAdapter.toSingleResponseEntity(libResponse);

        // 3. Проверка
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        // Здесь тело — это сам объект (String), так как toSingleResponseEntity распаковывает DataResponse
        assertEquals("Created Item", response.getBody());
    }

    @Test
    @DisplayName("ADAPTER: Конвертация DELETE (возврат числа)")
    void testToDeleteResponseEntity() {
        DataResponse<Long> libResponse = new DataResponse<>(
                List.of(5L), // Удалено 5 записей
                5L,
                Collections.emptyMap(),
                LoaderHttpStatus.OK
        );

        // Возвращает ResponseEntity<Long>
        var response = SpringResponseAdapter.toSingleResponseEntity(libResponse);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(5L, response.getBody());
    }

    @Test
    @DisplayName("ADAPTER: Конвертация файла (EXPORT)")
    void testToExportResponseEntity() {
        // 1. Ресурс от библиотеки
        ExportResource resource = new ExportResource(
                "report.xlsx",
                "application/vnd.ms-excel",
                new ByteArrayInputStream(new byte[]{1, 2, 3}),
                3,
                LoaderHttpStatus.OK
        );

        // 2. Конвертация (возвращает ResponseEntity<Object> или ResponseEntity<Resource>)
        var response = SpringResponseAdapter.toExportResponseEntity(resource);

        // 3. Проверка заголовков скачивания
        assertEquals(HttpStatus.OK, response.getStatusCode());

        List<String> dispositions = response.getHeaders().get(Constant.CONTENT_DISPOSITION);
        assertNotNull(dispositions);
        assertTrue(dispositions.get(0).contains("attachment"));
        assertTrue(dispositions.get(0).contains("report.xlsx"));

        assertEquals(3L, response.getHeaders().getContentLength());
        assertTrue(response.getBody() instanceof Resource);
    }

    @Test
    @DisplayName("ADAPTER: Обработка ошибки при экспорте (NO_CONTENT)")
    void testExportNoContent() {
        ExportResource resource = new ExportResource(
                "empty.xlsx",
                "application/octet-stream",
                null,
                0,
                LoaderHttpStatus.NO_CONTENT // Пустой отчет
        );

        var response = SpringResponseAdapter.toExportResponseEntity(resource);

        // Адаптер должен вернуть статус 204 без тела
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
    }
}
