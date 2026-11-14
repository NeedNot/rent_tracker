package net.neednot.renttracker.feature.tenants.domain

sealed interface LedgerBalance {
    data class Owed(
        val cents: Long,
    ) : LedgerBalance

    data class Credit(
        val cents: Long,
    ) : LedgerBalance

    data object PaidUp : LedgerBalance
}
