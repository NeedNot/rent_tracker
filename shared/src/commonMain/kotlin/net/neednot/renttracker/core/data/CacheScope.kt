package net.neednot.renttracker.core.data

object CacheScope {
    const val HOME = "home"
    const val ACCOUNT = "account"

    fun group(groupId: String): String = "group:$groupId"

    fun tenant(tenantId: String): String = "tenant:$tenantId"

    fun calendarYear(
        tenantId: String,
        year: Int,
    ): String = "calendar:$tenantId:$year"

    fun members(groupId: String): String = "members:$groupId"
}
