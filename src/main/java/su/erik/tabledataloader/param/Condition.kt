package su.erik.tabledataloader.param

import su.erik.tabledataloader.param.Filter.SqlSuffix

@SuppressWarnings("unused", "UnusedReturnValue")
sealed interface Condition<T : Condition<T>> {
    val field: String
    val suffix: SqlSuffix
    val intent: SqlSuffix
    val lBracket: String
    val rBracket: String

    // Переименовали set->with, чтобы избежать конфликта с var свойствами
    fun withSuffix(suffix: SqlSuffix): T
    fun withLBracket(lb: String): T
    fun withRBracket(rb: String): T


    fun open(): T {
        withSuffix(intent)
        return this as T
    }

    fun close(): T {
        withSuffix(SqlSuffix.CLOSE)
        return this as T
    }

    fun chain(): T {
        if (suffix == SqlSuffix.CLOSE) {
            open()
        }
        return this as T
    }

    fun addRBracket(): T {
        withRBracket(rBracket + ")")
        return this as T
    }

    fun addLBrackets(count: Int): T {
        if (count > 0) {
            // "(".repeat(n) работает быстро и эффективно в Kotlin/Java 11+
            return withLBracket("(".repeat(count) + lBracket)
        }
        return this as T
    }

    fun getSqlSuffix(): String {
        return if (suffix == SqlSuffix.CLOSE) "" else suffix.name
    }
}
