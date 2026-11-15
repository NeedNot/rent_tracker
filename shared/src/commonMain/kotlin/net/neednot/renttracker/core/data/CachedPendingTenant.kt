package net.neednot.renttracker.core.data

data class CachedPendingTenant(
    val groupId: String,
    val tenantName: String,
    val balanceCents: Long?,
)
