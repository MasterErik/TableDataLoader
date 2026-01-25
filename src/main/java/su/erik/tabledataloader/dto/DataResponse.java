package su.erik.tabledataloader.dto;

import java.util.List;
import java.util.Map;

public record DataResponse<T>(List<T> items, Long total, Map<String, String> headers, LoaderHttpStatus status) {
}
