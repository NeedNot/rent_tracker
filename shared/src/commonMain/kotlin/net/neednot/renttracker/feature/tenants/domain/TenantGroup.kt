package net.neednot.renttracker.feature.tenants.domain

data class TenantGroup(
    val name: String,
    val graceDays: Int,
    val lateFeeFlatCents: Long?,
)
