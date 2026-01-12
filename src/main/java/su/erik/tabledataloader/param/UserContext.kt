package su.erik.tabledataloader.param
@JvmRecord
data class UserContext(val id: Long?, val roles: List<String>?) {

    fun hasRole(role: String): Boolean {
        return roles?.contains(role) == true
    }

    companion object {
        fun empty() = UserContext(null, null)
    }
}
