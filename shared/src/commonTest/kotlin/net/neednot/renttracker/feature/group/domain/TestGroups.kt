package net.neednot.renttracker.feature.group.domain

import kotlinx.datetime.LocalDate

fun groupDetails(
    activeTenants: List<GroupTenant> = emptyList(),
    archivedTenants: List<GroupTenant> = emptyList(),
    totalOwedCents: Long = 0,
    lateTenantCount: Int = 0,
    archivedOwedCents: Long = 0,
) = GroupDetails(
    groupId = "g1",
    name = "1st Street",
    totalOwedCents = totalOwedCents,
    lateTenantCount = lateTenantCount,
    archivedOwedCents = archivedOwedCents,
    activeTenants = activeTenants,
    archivedTenants = archivedTenants,
)

fun groupTenant(
    tenantId: String,
    name: String = "Tenant $tenantId",
    contactName: String? = null,
    nextDueDate: LocalDate? = null,
    state: GroupTenantState = GroupTenantState.PaidUp,
    balance: TenantBalance = TenantBalance.PaidUp,
) = GroupTenant(
    tenantId = tenantId,
    name = name,
    contactName = contactName,
    nextDueDate = nextDueDate,
    state = state,
    balance = balance,
)
