package net.neednot.renttracker.feature.tenants.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.backend.Backend
import net.neednot.renttracker.core.data.AsOfClock
import net.neednot.renttracker.core.data.RentTrackerDatabase
import net.neednot.renttracker.core.data.ScreenResultStore
import net.neednot.renttracker.core.data.replaceCalendarYear
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.sync.syncedFlow
import net.neednot.renttracker.core.telemetry.Telemetry
import net.neednot.renttracker.feature.tenants.domain.MonthDetail
import net.neednot.renttracker.feature.tenants.domain.TenantCalendar
import net.neednot.renttracker.feature.tenants.domain.TenantCalendarRepository
import net.neednot.renttracker.feature.tenants.domain.TenantError
import net.neednot.renttracker.model.tenants.TenantsCalendarArgs
import net.neednot.renttracker.model.tenants.TenantsCalendarResult

class TenantCalendarRepositoryImpl(
    backend: Backend,
    private val asOfClock: AsOfClock,
    private val database: RentTrackerDatabase,
    private val screenResultStore: ScreenResultStore,
    telemetry: Telemetry,
) : TenantCalendarRepository {
    private val calls = TenantBackendCalls(backend, telemetry)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeCalendar(
        tenantId: String,
        yearOffset: Int,
    ): Flow<Synced<TenantCalendar, TenantError>> =
        database
            .observeCurrentYear(tenantId, asOfClock.asOf)
            .filterNotNull()
            .distinctUntilChanged()
            .flatMapLatest { currentYear ->
                val year = currentYear + yearOffset
                if (yearOffset == 0) {
                    database.observeCachedCalendar(tenantId, year).map { Synced(it, SyncStatus.Synced) }
                } else {
                    syncedYear(tenantId, year)
                }
            }

    override fun observeMonthDetail(
        tenantId: String,
        month: YearMonth,
    ): Flow<MonthDetail?> = database.observeMonthDetail(tenantId, month)

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun syncedYear(
        tenantId: String,
        year: Int,
    ): Flow<Synced<TenantCalendar, TenantError>> =
        syncedFlow(
            cached = database.observeCachedCalendar(tenantId, year),
            serverResults =
                asOfClock.asOf.flatMapLatest { asOf ->
                    calls.subscribe(
                        name = TENANT_CALENDAR,
                        args = TenantsCalendarArgs(asOf = asOf, tenantId = tenantId, year = year.toLong()),
                        argsSerializer = TenantsCalendarArgs.serializer(),
                        resultSerializer = TenantsCalendarResult.serializer(),
                    )
                },
            store = { result -> screenResultStore.replaceCalendarYear(tenantId, year, result) },
        )

    private companion object {
        const val TENANT_CALENDAR = "tenants:calendar"
    }
}
