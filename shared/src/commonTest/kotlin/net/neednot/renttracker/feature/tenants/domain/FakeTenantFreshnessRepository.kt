package net.neednot.renttracker.feature.tenants.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.sync.Freshness

class FakeTenantFreshnessRepository : TenantFreshnessRepository {
    val tenant = MutableStateFlow<Freshness>(Freshness.Online)
    val ledger = MutableStateFlow<Freshness>(Freshness.Online)
    val monthDetail = MutableStateFlow<Freshness>(Freshness.Online)

    override fun observeTenantFreshness(tenantId: String): Flow<Freshness> = tenant

    override fun observeLedgerFreshness(tenantId: String): Flow<Freshness> = ledger

    override fun observeMonthDetailFreshness(
        tenantId: String,
        month: YearMonth,
    ): Flow<Freshness> = monthDetail
}
