package su.erik.tabledataloader.param

import su.erik.tabledataloader.config.Constant

class Filter(
    override val field: String,
    op: String,
    value: Any?,
    val valueR: Any? = null,
    override val intent: SqlSuffix = SqlSuffix.AND
) : Condition<Filter> {

    // Свойства
    val op: String = op

    // ВАЖНО: modifyValue теперь достаточно умен, чтобы не ломать списки
    val value: Any? = modifyValue(op, value)

    // Mutable свойства (реализация интерфейса)
    override var suffix: SqlSuffix = SqlSuffix.CLOSE
        private set
    override var lBracket: String = ""
        private set
    override var rBracket: String = ""
        private set

    // --- MyBatis Helper ---
    // Это свойство позволит писать <when test="item.list"> вместо instanceof
    val isList: Boolean
        get() = value is List<*> && value.isNotEmpty()

    // --- Fluent Setters ---
    override fun withSuffix(suffix: SqlSuffix): Filter = apply { this.suffix = suffix }
    override fun withLBracket(lb: String): Filter = apply { this.lBracket = lb }
    override fun withRBracket(rb: String): Filter = apply { this.rBracket = rb }

    enum class SqlSuffix { OR, AND, CLOSE }

    companion object {
        private fun modifyValue(op: String, value: Any?): Any? {
            if (value == null) return null

            // ВАЖНО: Если это список, возвращаем его как объект, не превращая в строку
            if (value is List<*>) {
                return value
            }

            val s = value.toString()
            return when {
                op.equals(Constant.LIKE, ignoreCase = true) || op.equals(Constant.ILIKE, ignoreCase = true) -> {
                    if (s.contains(Constant.SEQUENCE)) s else "${Constant.SEQUENCE}$s${Constant.SEQUENCE}"
                }
                op.equals(Constant.END_LIKE, ignoreCase = true) || op.equals(Constant.END_ILIKE, ignoreCase = true) -> {
                    "$value${Constant.SEQUENCE}"
                }
                else -> value
            }
        }
    }

    fun self(): Filter = this // Если нужно для дженериков, хотя с sealed interface мб уже не актуально
}
