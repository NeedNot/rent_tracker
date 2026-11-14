package net.neednot.renttracker.feature.group.domain

import kotlinx.datetime.LocalDate

data class GroupTenant(
    val tenantId: String,
    val name: String,
    val contactName: String?,
    val nextDueDate: LocalDate?,
    val state: GroupTenantState,
    val balance: TenantBalance,
)
