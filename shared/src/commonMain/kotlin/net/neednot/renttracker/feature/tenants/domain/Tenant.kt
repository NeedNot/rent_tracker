package net.neednot.renttracker.feature.tenants.domain

data class Tenant(
    val tenantId: String,
    val groupId: String,
    val isArchived: Boolean,
    val version: Long,
    val details: TenantDetails,
)
