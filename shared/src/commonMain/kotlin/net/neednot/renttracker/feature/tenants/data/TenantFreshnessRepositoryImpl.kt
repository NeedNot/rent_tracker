package net.neednot.renttracker.feature.tenants.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.data.AsOfClock
import net.neednot.renttracker.core.data.CacheScope
import net.neednot.renttracker.core.data.FreshnessSource
import net.neednot.renttracker.core.data.RentTrackerDatabase
import net.neednot.renttracker.core.sync.Freshness
import net.neednot.renttracker.feature.tenants.domain.TenantFreshnessRepository

@OptIn(ExperimentalCoroutinesApi::class)
class TenantFreshnessRepositoryImpl(
    private val database: RentTrackerDatabase,
    private val asOfClock: AsOfClock,
    private val freshnessSource: FreshnessSource,
) : TenantFreshnessRepository {
    override fun observeTenantFreshness(tenantId: String): Flow<Freshness> =
        database
            .tenantDao()
            .observeById(tenantId)
            .map { it?.groupId }
            .distinctUntilChanged()
            .flatMapLatest { groupId ->
                freshnessSource.observe(listOfNotNull(CacheScope.tenant(tenantId), groupId?.let(CacheScope::group)))
            }

    override fun observeLedgerFreshness(tenantId: String): Flow<Freshness> =
        freshnessSource.observe(listOf(CacheScope.tenant(tenantId)))

    override fun observeMonthDetailFreshness(
        tenantId: String,
        month: YearMonth,
    ): Flow<Freshness> =
        database
            .observeCurrentYear(tenantId, asOfClock.asOf)
            .distinctUntilChanged()
            .flatMapLatest { currentYear ->
                val yearScope = CacheScope.calendarYear(tenantId, month.year).takeIf { month.year != currentYear }
                freshnessSource.observe(listOfNotNull(CacheScope.tenant(tenantId), yearScope))
            }
}
