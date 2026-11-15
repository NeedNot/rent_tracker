package net.neednot.renttracker.feature.tenants.domain

data class PendingBalanceChange(
    val wasCents: Long,
    val nowCents: Long,
)
