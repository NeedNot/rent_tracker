package net.neednot.renttracker.feature.tenants.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.sync.Freshness

interface TenantFreshnessRepository {
    fun observeTenantFreshness(tenantId: String): Flow<Freshness>

    fun observeLedgerFreshness(tenantId: String): Flow<Freshness>

    fun observeMonthDetailFreshness(
        tenantId: String,
        month: YearMonth,
    ): Flow<Freshness>
}
