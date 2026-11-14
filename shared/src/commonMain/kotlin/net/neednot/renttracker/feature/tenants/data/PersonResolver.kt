package net.neednot.renttracker.feature.tenants.data

import net.neednot.renttracker.core.data.AccountNameEntity
import net.neednot.renttracker.feature.tenants.domain.CallerPermissions
import net.neednot.renttracker.feature.tenants.domain.Person

internal class PersonResolver(
    accountNames: List<AccountNameEntity>,
    private val permissions: CallerPermissions?,
) {
    private val namesById = accountNames.associate { it.accountId to it.displayName }

    fun personOf(accountId: String?): Person =
        when {
            accountId != null && permissions?.isYou(accountId) == true -> Person.You
            else -> Person.Named(namesById[accountId] ?: DELETED_USER)
        }

    private companion object {
        const val DELETED_USER = "Deleted user"
    }
}
