package su.erik.tabledataloader.config;

import java.util.Map;
import java.util.function.Function;

public enum StandardParam {

    // --- Pagination ---
    PER_PAGE("limit", "X-Pagination-Per-Page", Constant.DEFAULT_PER_PAGE),
    CURRENT_PAGE("page", "X-Pagination-Current-Page", Constant.DEFAULT_PAGE),
    IS_CUSTOM_PAGINATION("customPagination", "X-Pagination-Custom", false),

    // --- Sorting ---
    SORT_FIELD("sortField", "X-Sort-Field", "id"),
    SORT_ORDER("sortOrder", "X-Sort-Order", Constant.SORT_DIRECTION.ASC.name()),

    // --- Search ---
    KEYWORD_SEARCH("keyword", "X-Keyword-Search", null),
    // Добавил заголовок для типа поиска, так как в списке констант его явно не было, но Enum есть
    KEYWORD_SEARCH_TYPE("keywordType", "X-Keyword-Search-Type", Constant.KEYWORD_SEARCH_TYPE.String.name()),

    // --- Internal (Import/System) ---
    HEADER_ROW_NUMBER("headerRowNumber", null, 0),
    COLUMN_MAPPER("columnMapper", null, null),
    FILE("file", null, null),
    ENTITY("entity", null, null),
    USER_ID("userId", null, null),
    USER_ROLES("userRoles", null, null);


    private final String key;          // Ключ в MapParam.filters
    private final String headerName;   // Имя HTTP заголовка
    private final Object defaultValue; // Значение по умолчанию

    StandardParam(String key, String headerName, Object defaultValue) {
        this.key = key;
        this.headerName = headerName;
        this.defaultValue = defaultValue;
    }

    public String getKey() {
        return key;
    }

    public String getHeaderName() {
        return headerName;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    /**
     * Извлекает значение из Map (filters) или возвращает default.
     * <p>
     * Метод инкапсулирует логику безопасного приведения типов и возврата значения по умолчанию,
     * если ключ отсутствует в карте.
     */
    @SuppressWarnings("unchecked")
    public <T> T getFrom(Map<String, Object> filters) {
        Object value = filters.get(this.key);
        if (value == null) {
            return (T) defaultValue;
        }
        return (T) value;
    }

    /**
     * Метод для безопасного парсинга значений из заголовков (строк).
     */
    public Object parseValue(String headerValue) {
        if (headerValue == null) return defaultValue;

        // Особая логика для Integer (пагинация)
        if (this == PER_PAGE || this == CURRENT_PAGE || this == HEADER_ROW_NUMBER) {
            try {
                int val = Integer.parseInt(headerValue);
                // Валидация MAX_PER_PAGE
                if (this == PER_PAGE && val > Constant.MAX_PER_PAGE) {
                    return Constant.MAX_PER_PAGE;
                }
                return val;
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        // Логика для Boolean
        if (this == IS_CUSTOM_PAGINATION) {
            return Boolean.parseBoolean(headerValue);
        }

        // По умолчанию возвращаем строку
        return headerValue;
    }
}
