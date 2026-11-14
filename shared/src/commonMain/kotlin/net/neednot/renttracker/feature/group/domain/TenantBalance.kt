package net.neednot.renttracker.feature.group.domain

sealed interface TenantBalance {
    data class Owes(
        val cents: Long,
    ) : TenantBalance

    data class Credit(
        val cents: Long,
    ) : TenantBalance

    data object PaidUp : TenantBalance
}
