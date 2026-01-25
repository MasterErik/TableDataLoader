package su.erik.tabledataloader.utils;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import su.erik.tabledataloader.config.Constant;
import su.erik.tabledataloader.dto.DataResponse;
import su.erik.tabledataloader.dto.ExportResource;
import su.erik.tabledataloader.dto.LoaderHttpStatus;

public class ResponseConverter {

    public static <T> ResponseEntity<DataResponse<T>> toResponseEntity(DataResponse<T> response) {
        return ResponseEntity
                .status(mapStatus(response.status()))
                .headers(h -> response.headers().forEach(h::add))
                .body(response);
    }

    public static ResponseEntity<Object> toResponseEntity(ExportResource resource) {
        if (resource.status().series() != LoaderHttpStatus.Series.SUCCESSFUL) {
            return ResponseEntity.status(mapStatus(resource.status())).build();
        }
        return ResponseEntity
                .status(mapStatus(resource.status()))
                .header(Constant.CONTENT_DISPOSITION, String.format(Constant.ATTACHMENT_FILENAME, resource.fileName()))
                .header(Constant.CONTENT_TYPE, resource.contentType())
                .contentLength(resource.size())
                .body(new InputStreamResource(resource.stream()));
    }

    private static HttpStatus mapStatus(LoaderHttpStatus status) {
        return HttpStatus.valueOf(status.value());
    }
}
