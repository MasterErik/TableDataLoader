package su.erik.tabledataloader.dto;

import java.util.List;
import java.util.Map;

public class DataResponse<T> {
    private final List<T> items;
    private final Long total;
    private final Map<String, String> headers;
    private final LoaderHttpStatus status;

    public DataResponse(List<T> items, Long total, Map<String, String> headers, LoaderHttpStatus status) {
        this.items = items;
        this.total = total;
        this.headers = headers;
        this.status = status;
    }

    public List<T> getItems() { return items; }
    public Long getTotal() { return total; }
    public Map<String, String> getHeaders() { return headers; }
    public LoaderHttpStatus getStatus() { return status; }
}
