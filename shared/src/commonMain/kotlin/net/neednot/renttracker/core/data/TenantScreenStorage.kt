package net.neednot.renttracker.core.data

import net.neednot.renttracker.model.tenants.TenantsCalendarResult
import net.neednot.renttracker.model.tenants.TenantsScreenResult

suspend fun ScreenResultStore.replaceTenant(
    tenantId: String,
    result: TenantsScreenResult,
) {
    replaceTenant(
        tenantId = tenantId,
        groups = result.groups.map { it.toEntity() },
        tenants = result.tenants.map { it.toEntity() },
        tenantSummaries = result.tenantSummaries.map { it.toEntity() },
        scope =
            TenantScopeEntities(
                rentTerms = result.rentTerms.map { it.toEntity() },
                payments = result.payments.map { it.toEntity() },
                debts = result.debts.map { it.toEntity() },
                ledgerRows = result.ledgerRows.map { it.toEntity() },
                accountNames = result.accounts.map { it.toEntity() },
                memberships = result.memberships.map { it.toEntity() },
                allocations = result.allocations.map { it.toEntity() },
                calendarMonths = result.calendarMonths.map { it.toEntity() },
            ),
    )
}

suspend fun ScreenResultStore.replaceCalendarYear(
    tenantId: String,
    year: Int,
    result: TenantsCalendarResult,
) {
    replaceCalendarYear(tenantId, year, result.calendarMonths.map { it.toEntity() })
}
