package su.erik.tabledataloader.param

import su.erik.tabledataloader.config.Constant

class TableSearch {

    private val columnSearchList = ArrayList<ColumnSearch>()

    // При установке значения сразу запускаем анализ типа
    var keywordSearch: String = ""
        set(value) {
            field = value
            analyzeAndPrepare()
        }

    var keywordLikeSearch: String = ""
        private set

    var keywordSearchType: String = Constant.KEYWORD_SEARCH_TYPE.String.name
        private set

    // Основной метод добавления колонок
    fun add(columnSearch: ColumnSearch) {
        columnSearch.tableSearch = this
        this.columnSearchList.add(columnSearch)
    }

    fun addAll(list: List<ColumnSearch>) {
        list.forEach { add(it) }
    }

    // Возвращает только те колонки, которые подходят под тип введенных данных
    fun toList(): List<ColumnSearch> {
        return columnSearchList.filter { it.typeSupported() }
    }

    // --- Внутренняя логика анализа ---

    private fun analyzeAndPrepare() {
        fillKeywordSearchType()
        prepareKeywordSearchForDoubleType()
        prepareKeywordSearchForLike()
    }

    private fun fillKeywordSearchType() {
        keywordSearchType = when {
            Constant.DOUBLE_PATTERN.matcher(keywordSearch).matches() -> Constant.KEYWORD_SEARCH_TYPE_DOUBLE
            Constant.INTEGER_PATTERN.matcher(keywordSearch).matches() -> Constant.KEYWORD_SEARCH_TYPE_INTEGER
            else -> Constant.KEYWORD_SEARCH_TYPE.String.name
        }
    }

    private fun prepareKeywordSearchForDoubleType() {
        if (keywordSearchType == Constant.KEYWORD_SEARCH_TYPE.Double.name) {
            try {
                keywordSearch = keywordSearch.toDouble().toString()
            } catch (e: NumberFormatException) {
                // Игнорируем, остаемся со строкой
            }
        }
    }

    private fun prepareKeywordSearchForLike() {
        keywordLikeSearch = if (keywordSearchType == Constant.KEYWORD_SEARCH_TYPE.String.name) {
            var search = keywordSearch
            if (!search.startsWith(Constant.SEQUENCE)) {
                search = "${Constant.SEQUENCE}$search"
            }
            if (!search.endsWith(Constant.SEQUENCE)) {
                search = "$search${Constant.SEQUENCE}"
            }
            search
        } else {
            "${Constant.SEQUENCE}$keywordSearch${Constant.SEQUENCE}"
        }
    }
}
