package net.neednot.renttracker.feature.tenants.domain

import net.neednot.renttracker.core.data.AccessLevel

data class CallerPermissions(
    val accountId: String,
    val isOwner: Boolean,
    val payments: AccessLevel,
    val debts: AccessLevel,
    val editOthers: Boolean,
) {
    val canLogPayments: Boolean get() = isOwner || payments >= AccessLevel.AddEdit

    val canAddCharges: Boolean get() = isOwner || debts >= AccessLevel.AddEdit

    fun canChangePayment(loggedBy: String): Boolean = mayChange(payments, addedBy = loggedBy)

    fun canChangeCharge(addedBy: String): Boolean = mayChange(debts, addedBy = addedBy)

    fun isYou(accountId: String): Boolean = accountId == this.accountId

    private fun mayChange(
        level: AccessLevel,
        addedBy: String,
    ): Boolean =
        isOwner ||
            level == AccessLevel.Full ||
            (level == AccessLevel.AddEdit && (addedBy == accountId || editOthers))
}
