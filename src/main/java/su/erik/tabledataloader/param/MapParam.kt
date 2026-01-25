package su.erik.tabledataloader.param

import com.puls.centralpricing.common.exception.InvalidInputParameterException
import su.erik.tabledataloader.config.Constant
import su.erik.tabledataloader.config.StandardParam
import su.erik.tabledataloader.param.Filter.SqlSuffix

@Suppress("unused", "UnusedReturnValue", "UNCHECKED_CAST")
open class MapParam {

    // =================================================================================================================
    //                                         DATA STORAGE
    // =================================================================================================================

    /**
     * Основная карта параметров (Limit, Offset, UserId, Custom Filters).
     */
    val filters: MutableMap<String, Any?> = HashMap()

    /**
     * Основной список критериев фильтрации (WHERE).
     */
    val criteria = ArrayList<Filter>()

    /**
     * Список параметров сортировки.
     */
    val orderBy = ArrayList<SortParam>()

    /**
     * Делегат для работы с поиском по колонкам.
     */
    private val tableSearch = TableSearch()
    val columns: List<ColumnSearch> get() = tableSearch.toList()

    // =================================================================================================================
    //                                         INTERNAL STATE & CONTEXT
    // =================================================================================================================

    private var lastAddedCondition: Filter? = null
    private var pendingOpenBrackets = 0

    var filialFilterColumns: MutableList<String>? = null
        private set

    var masterListId: List<Any>? = null
        private set

    // =================================================================================================================
    //                                         CONSTRUCTORS
    // =================================================================================================================

    constructor()

    constructor(limit: Int) : this() {
        this.limit = limit
    }

    constructor(value: List<*>, name: String) : this() {
        addCriteriaIn(name, value)
    }

    constructor(fieldName: String, operation: String, value: Any?) : this() {
        addCriteria(fieldName, operation, value)
    }

    constructor(fieldName: String, operation: String, value: Any?, suffix: Enum<SqlSuffix>) : this() {
        addCriteria(fieldName, operation, value, suffix as SqlSuffix)
    }

    constructor(name: String, value: Any?) : this() {
        filter(name, value)
    }

    constructor(value: Any?) : this() {
        filter(value)
    }

    // =================================================================================================================
    //                                         PROPERTIES (RAW ACCESS)
    // =================================================================================================================

    // Геттеры возвращают null, если значение не задано (важно для валидации check())
    var limit: Int?
        get() = StandardParam.PER_PAGE.getFrom(filters)
        set(value) { filter(StandardParam.PER_PAGE.key, value) }

    var offset: Int?
        get() = StandardParam.CURRENT_PAGE.getFrom(filters)
        set(value) { filter(StandardParam.CURRENT_PAGE.key, value) }

    var userId: Long?
        get() = StandardParam.USER_ID.getFrom(filters)
        set(value) { filter(StandardParam.USER_ID.key, value) }

    var userRoles: List<String>?
        get() = StandardParam.USER_ROLES.getFrom(filters)
        set(value) { filter(StandardParam.USER_ROLES.key, value) }

    // --- Keyword Search ---

    var keywordSearch: String?
        get() = if (tableSearch.keywordSearch.isEmpty()) null else tableSearch.keywordSearch
        set(value) {
            if (value != null) {
                tableSearch.keywordSearch = value
            }
        }

    val keywordSearchType: String
        get() = tableSearch.keywordSearchType

    // =================================================================================================================
    //                                         CRITERIA LOGIC
    // =================================================================================================================

    fun openBracket(): MapParam = apply { pendingOpenBrackets++ }

    fun closeBracket(): MapParam = apply { lastAddedCondition?.addRBracket() }

    @JvmOverloads
    fun addCriteria(fieldName: String, operation: String, value: Any?, suffix: SqlSuffix = SqlSuffix.AND, valueR: Any? = null): MapParam = apply {
        if (value != null) {
            lastAddedCondition?.chain()
            val newFilter = Filter(fieldName, operation, value, valueR, suffix)
            newFilter.addLBrackets(pendingOpenBrackets)
            pendingOpenBrackets = 0
            newFilter.close()
            criteria.add(newFilter)
            lastAddedCondition = newFilter
        }
    }

    // --- Shortcuts ---

    fun addCriteria(fieldName: String, value: Any?): MapParam {
        if (value == null) return this
        return if (value is List<*>) {
            addCriteriaIn(fieldName, value)
        } else {
            addCriteria(fieldName, "=", value)
        }
    }

    fun addCriteria(fieldName: String, operation: String, value: Any?, valueR: Any?): MapParam {
        if (value != null) addCriteria(fieldName, operation, value, SqlSuffix.AND, valueR)
        return this
    }

    @JvmOverloads
    fun addCriteriaIn(fieldName: String, value: List<*>?, suffix: SqlSuffix = SqlSuffix.AND, inOperation: String = Constant.IN): MapParam = apply {
        if (!value.isNullOrEmpty()) addCriteria(fieldName, inOperation, value, suffix)
    }

    /**
     * Добавляет кастомный параметр, который будет доступен в MyBatis через mapParam.filters['key'].
     * Включает защиту от перезаписи системных полей.
     */
    fun addCustomFilter(key: String, value: Any?): MapParam = apply {
        if (value != null) {
            // ЗАЩИТА: Проверяем, не является ли ключ системным (limit, offset и т.д.)
            if (StandardParam.entries.any { it.key == key }) {
                throw IllegalArgumentException("Cannot use system key '$key' as a custom filter. It is reserved.")
            }
            filters[key] = value
        }
    }

    // =================================================================================================================
    //                                         SORTING
    // =================================================================================================================

    /**
     * Добавляет сортировку по строке (парсит через fromString).
     */
    fun addOrderBy(sortBy: String, sortOrder: String) {
        val direction = SortDirection.fromString(sortOrder)
        orderBy.add(SortParam(sortBy, direction))
    }

    /**
     * Добавляет сортировку напрямую через Enum.
     */
    fun addOrderBy(sortBy: String, sortOrder: SortDirection) {
        orderBy.add(SortParam(sortBy, sortOrder))
    }

    fun clearOrderBy() {
        orderBy.clear()
    }

    // =================================================================================================================
    //                                         OTHER CONFIG METHODS
    // =================================================================================================================

    fun filter(name: String, value: Any?): MapParam = apply {
        if (value != null) filters[name] = value
    }

    fun filter(value: Any?): MapParam = apply {
        if (value != null) filter(value::class.java.simpleName, value)
    }

    fun setTable(name: String): MapParam = filter(Constant.TABLE_NAME, name)

    fun setMasterListId(ids: List<Any>?): MapParam = apply { this.masterListId = ids }

    fun addFilialFilterColumn(column: String) = apply {
        if (filialFilterColumns == null) filialFilterColumns = ArrayList()
        filialFilterColumns?.add(column)
    }

    fun setColumns(vararg columns: String): MapParam = apply { columns.forEach { tableSearch.add(ColumnSearch(it)) } }

    fun setColumns(vararg columns: ColumnSearch): MapParam = apply { columns.forEach { tableSearch.add(it) } }

    fun clearFilters() {
        filters.clear()
    }

    fun clearCriteria() {
        criteria.clear()
        lastAddedCondition = null
        pendingOpenBrackets = 0
    }

    // =================================================================================================================
    //                                         VALIDATION & LIFECYCLE
    // =================================================================================================================

    fun prepareValue(): MapParam {
        lastAddedCondition?.close()
        return this
    }

    /**
     * Проверяет валидность параметров.
     * Правило U007: Если пагинация задана ЯВНО, то должна быть задана сортировка.
     */
    fun check() {
        // Мы используем containsKey, потому что геттер (getFrom) может вернуть дефолтное значение (например 20).
        // Ошибка должна падать, только если пользователь явно запросил пагинацию без сортировки.
        val hasLimit = filters.containsKey(StandardParam.PER_PAGE.key)
        val hasOffset = filters.containsKey(StandardParam.CURRENT_PAGE.key)
        val hasSorting = orderBy.isNotEmpty()

        if ((hasLimit || hasOffset) && !hasSorting) {
            throw InvalidInputParameterException("Pagination requires Sorting parameters. Error code: U007")
        }
    }

    fun self(): MapParam = this

    // =================================================================================================================
    //                                         NESTED CLASSES
    // =================================================================================================================

    data class SortParam(val sortBy: String, val sortOrder: SortDirection) {
        override fun toString(): String = "$sortBy $sortOrder"
    }

    enum class SortDirection {
        ASC,
        DESC;

        companion object {
            @JvmStatic
            fun fromString(value: String?): SortDirection {
                if (value.isNullOrBlank()) return ASC
                return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: ASC
            }
        }
    }
}
