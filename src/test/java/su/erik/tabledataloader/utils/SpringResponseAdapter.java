package su.erik.tabledataloader.utils; // Или ваш пакет адаптеров

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import su.erik.tabledataloader.config.Constant;
import su.erik.tabledataloader.dto.DataResponse;
import su.erik.tabledataloader.dto.ExportResource;
import su.erik.tabledataloader.dto.LoaderHttpStatus;

/**
 * Адаптер для преобразования ответов библиотеки (DataResponse, ExportResource)
 * в объекты ответов Spring Framework (ResponseEntity).
 */
public class SpringResponseAdapter {

    /**
     * Для списка данных (GET).
     */
    public static <T> ResponseEntity<DataResponse<T>> toListResponseEntity(DataResponse<T> response) {
        return ResponseEntity
                .status(mapStatus(response.status()))
                .headers(toHttpHeaders(response))
                .body(response);
    }

    /**
     * Для одиночной сущности (POST, PUT, GET by ID).
     * Возвращает ResponseEntity<T>, извлекая первый элемент из списка.
     */
    public static <T> ResponseEntity<T> toSingleResponseEntity(DataResponse<T> response) {
        T body = null;
        if (response.items() != null && !response.items().isEmpty()) {
            body = response.items().getFirst();
        }
        return ResponseEntity
                .status(mapStatus(response.status()))
                .headers(toHttpHeaders(response))
                .body(body);
    }

    /**
     * Для экспорта файлов (Excel, CSV, Zip).
     */
    public static ResponseEntity<Object> toExportResponseEntity(ExportResource resource) {
        // 1. Если статус не успешный (ошибка).
        // 2. ИЛИ если статус NO_CONTENT (204) — это успех, но тела быть не должно.
        // 3. ИЛИ если поток физически отсутствует (null).
        if (resource.status().series() != LoaderHttpStatus.Series.SUCCESSFUL ||
                resource.status() == LoaderHttpStatus.NO_CONTENT ||
                resource.stream() == null) {

            return ResponseEntity.status(mapStatus(resource.status())).build();
        }

        return ResponseEntity
                .status(mapStatus(resource.status()))
                .header(Constant.CONTENT_DISPOSITION, String.format("attachment; filename=\"%s\"", resource.fileName()))
                .header(Constant.CONTENT_TYPE, resource.contentType())
                .contentLength(resource.size())
                .body(new InputStreamResource(resource.stream()));
    }

    // --- Вспомогательные методы ---
    private static HttpStatus mapStatus(LoaderHttpStatus status) {
        return HttpStatus.valueOf(status.value());
    }

    private static HttpHeaders toHttpHeaders(DataResponse<?> response) {
        HttpHeaders headers = new HttpHeaders();
        response.headers().forEach(headers::add);
        return headers;
    }
}
