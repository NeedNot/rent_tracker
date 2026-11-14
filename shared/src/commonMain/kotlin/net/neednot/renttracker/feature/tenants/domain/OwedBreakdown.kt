package net.neednot.renttracker.feature.tenants.domain

data class OwedBreakdown(
    val rentCents: Long,
    val lateFeesCents: Long,
    val otherCents: Long,
)
