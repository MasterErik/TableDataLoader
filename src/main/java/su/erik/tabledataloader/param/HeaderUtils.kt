package su.erik.tabledataloader.param

import su.erik.tabledataloader.config.Constant
import su.erik.tabledataloader.config.StandardParam
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.function.Function
import kotlin.math.max

object HeaderUtils {

    @JvmStatic
    fun fillMapParam(mapParam: MapParam, headerAccessor: Function<String?, String?>) {
        processPagination(mapParam, headerAccessor)
        processSorting(mapParam, headerAccessor)
        processKeyword(mapParam, headerAccessor)
        processCustomParams(mapParam, headerAccessor)
    }

    private fun processPagination(mapParam: MapParam, accessor: Function<String?, String?>) {
        val perPageStr = accessor.apply(StandardParam.PER_PAGE.headerName)
        val pageStr = accessor.apply(StandardParam.CURRENT_PAGE.headerName)

        val limit = StandardParam.PER_PAGE.parseValue(perPageStr) as Int?
        val page = StandardParam.CURRENT_PAGE.parseValue(pageStr) as Int?

        if (limit != null) {
            mapParam.limit = limit
        } else if (page != null) {
            mapParam.limit = Constant.DEFAULT_PER_PAGE
        }

        if (page != null) {
            val actualLimit = mapParam.limit ?: Constant.DEFAULT_PER_PAGE
            val safePage = max(page, 1)
            mapParam.offset = (safePage - 1) * actualLimit
        } else if (mapParam.limit != null && mapParam.offset == null) {
            mapParam.offset = Constant.DEFAULT_PAGE
        }
    }

    private fun processSorting(mapParam: MapParam, accessor: Function<String?, String?>) {
        val sortField = accessor.apply(StandardParam.SORT_FIELD.headerName)
        val sortOrder = accessor.apply(StandardParam.SORT_ORDER.headerName)

        if (sortField != null && sortOrder != null) {
            // Передаем строки, MapParam внутри разберет их через SortDirection.fromString
            mapParam.addOrderBy(sortField, sortOrder)
        }
    }

    private fun processKeyword(mapParam: MapParam, accessor: Function<String?, String?>) {
        val rawKeyword = accessor.apply(StandardParam.KEYWORD_SEARCH.headerName)
        if (rawKeyword != null) {
            try {
                val keyword = URLDecoder.decode(rawKeyword, StandardCharsets.UTF_8)
                mapParam.keywordSearch = keyword

                val type = accessor.apply(StandardParam.KEYWORD_SEARCH_TYPE.headerName)
                if (type != null) {
                    mapParam.filter(StandardParam.KEYWORD_SEARCH_TYPE.key, type)
                }
            } catch (ignored: Exception) { }
        }
    }

    private fun processCustomParams(mapParam: MapParam, accessor: Function<String?, String?>) {
        val custom = accessor.apply(StandardParam.IS_CUSTOM_PAGINATION.headerName)
        if (custom != null) {
            mapParam.filter(StandardParam.IS_CUSTOM_PAGINATION.key, custom.toBoolean())
        }
    }

    @JvmStatic
    fun createResponseHeaders(mapParam: MapParam, totalCount: Long): MutableMap<String?, String?> {
        val headers: MutableMap<String?, String?> = HashMap()
        headers[Constant.X_PAGINATION_TOTAL_ENTRIES] = totalCount.toString()

        val currentLimit = mapParam.limit
        if (currentLimit != null && currentLimit > 0) {
            headers[StandardParam.PER_PAGE.headerName] = currentLimit.toString()
            val currentOffset = mapParam.offset
            if (currentOffset != null) {
                val currentPage = (currentOffset / currentLimit) + 1
                headers[StandardParam.CURRENT_PAGE.headerName] = currentPage.toString()
            }
        }
        return headers
    }
}
