package su.erik.tabledataloader.param

import su.erik.tabledataloader.config.Constant

class ColumnSearch(
    val name: String,
    // В Kotlin списки неизменяемы по умолчанию, это хорошо
    val supportedTypes: List<Constant.KEYWORD_SEARCH_TYPE>,
    val operator: String
) {
    // Ссылка на родительский TableSearch (lateinit, так как устанавливается после создания)
    internal lateinit var tableSearch: TableSearch

    // --- Конструкторы для удобства (как в Java версии) ---

    constructor(name: String) : this(
        name,
        Constant.KEYWORD_SEARCH_TYPE.entries,
        Constant.LIKE
    )

    constructor(name: String, supportedType: Constant.KEYWORD_SEARCH_TYPE) : this(
        name,
        listOf(supportedType),
        when (supportedType) {
            Constant.KEYWORD_SEARCH_TYPE.String -> Constant.LIKE
            Constant.KEYWORD_SEARCH_TYPE.Integer, Constant.KEYWORD_SEARCH_TYPE.Double -> Constant.EQUALS
        }
    )

    constructor(name: String, supportedType: Constant.KEYWORD_SEARCH_TYPE, operator: String) : this(
        name,
        listOf(supportedType),
        operator
    )

    // --- Логика ---

    val keywordSearch: String
        get() = tableSearch.keywordSearch

    val isLikeOperator: Boolean
        get() = operator.equals(Constant.LIKE, ignoreCase = true) ||
                operator.equals(Constant.ILIKE, ignoreCase = true) ||
                operator.equals(Constant.END_LIKE, ignoreCase = true) ||
                operator.equals(Constant.END_ILIKE, ignoreCase = true)

    fun typeSupported(): Boolean {
        // Проверяем, поддерживает ли колонка текущий тип глобального поиска
        val currentType = tableSearch.keywordSearchType
        return supportedTypes.any { type ->
            type.name == currentType || type == Constant.KEYWORD_SEARCH_TYPE.String
        }
    }

    override fun toString(): String = name
}
