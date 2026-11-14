package net.neednot.renttracker.feature.tenants.domain

import kotlinx.datetime.LocalDate

data class TenantOverview(
    val tenant: Tenant,
    val group: TenantGroup,
    val balance: TenantBalance?,
    val activeRentTerms: List<RentTerm>,
    val today: LocalDate,
    val ledger: List<LedgerEntry>,
    val permissions: CallerPermissions?,
    val lateFeeSuggestion: LateFeeSuggestion?,
)
