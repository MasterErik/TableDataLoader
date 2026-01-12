package su.erik.tabledataloader.config;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Application constants.
 */
public final class Constant {

    public static final String DEFAULT_CRITERIA = "filters";
    public static final String EQUALS = "=";
    public static final String SPACE_SEPARATOR = " ";
    public static final String LIKE = "LIKE";
    public static final String END_LIKE = LIKE + SPACE_SEPARATOR;
    public static final String ILIKE = "ILIKE";
    public static final String END_ILIKE = ILIKE + SPACE_SEPARATOR;
    public static final String IN = "IN";
    public static final String NOT_IN = "NOT IN";
    public static final String SEQUENCE = "%";
    public static final String BETWEEN = "BETWEEN";

    public static final String MASTER_ID = "masterId";
    public static final String EXPANDED_KEY = "expandedKey";

    public static final String X_PAGINATION_TOTAL_ENTRIES = "X-Pagination-Total-Entries";

// --- Defaults & Limits ---
    public static final int DEFAULT_PER_PAGE = 20;
    public static final int DEFAULT_PAGE = 0;
    public static final int MAX_PER_PAGE = 500;

    public static final String TABLE_NAME = "tableName";
    public static final String CONTENT_TYPE_OCTET_STREAM = "application/octet-stream";
    public static final String CONTENT_DISPOSITION = "Content-Disposition";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String ATTACHMENT_FILENAME = "attachment; filename=\"%s\"";

    public enum SORT_DIRECTION {
        DESC,
        ASC,
    }

    public enum KEYWORD_SEARCH_TYPE {
        Integer,
        Double,
        String
    }

    public static final Pattern DOUBLE_PATTERN = Pattern.compile("([0-9]*)\\.([0-9]*)", Pattern.CASE_INSENSITIVE);
    public static final Pattern INTEGER_PATTERN = Pattern.compile("([0-9]*)", Pattern.CASE_INSENSITIVE);

    public static final String KEYWORD_SEARCH_TYPE_INTEGER = KEYWORD_SEARCH_TYPE.Integer.name();
    public static final String KEYWORD_SEARCH_TYPE_DOUBLE = KEYWORD_SEARCH_TYPE.Double.name();

    public static final String FILE_PARAM = "file";
    public static final String ENTITY_PARAM = "entity";

    public static final String DELIMITER = "@";

    public static final List<String> supportedFileExtensions = List.of("csv", "ods", "xls", "xlsx", "xlsb");

    public static final List<String> supportedArchiveExtensions = List.of("zip", "rar", "7z");
    public static final String ZIP = ".zip";
    public static final String RAR = ".rar";
    public static final String SEVEN_Z = ".7z";

    public static final String UNSUPPORTED_FORMAT_ERROR_MSG = "Unsupported file format";
    public static final String ARCHIVE_IS_EMPTY_ERROR_MSG = "Archive is empty!";
    public static final String FILE_READING_ERROR_MSG = "File reading error, %s";
    public static final String FILE_MISSING_MSG = "File is missing";
    public static final String RESULT_DATASET_IS_EMPTY_MSG = "Result dataset to export is empty";

    public static final String CONTENT_TYPE_ARCHIVE = "archive";
    public static final String TEMP_ARCHIVE_NAME = "temp-archive-";

    public static final String FILE_NAME_FILTER = "fileName";
    public static final String DATE_FILTER = "date";

    public static final String IMPORT_MAPPER_NOT_SPECIFIED_MSG = "ImportMapper for TableDataLoader wasn't specified";

    private Constant() {}
}
