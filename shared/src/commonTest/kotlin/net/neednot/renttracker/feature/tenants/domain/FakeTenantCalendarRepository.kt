package net.neednot.renttracker.feature.tenants.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced

class FakeTenantCalendarRepository : TenantCalendarRepository {
    val calendar =
        MutableStateFlow<Synced<TenantCalendar, TenantError>>(
            Synced(TenantCalendar(2026, emptyList()), SyncStatus.Synced),
        )
    val monthDetail = MutableStateFlow<MonthDetail?>(null)

    val requestedYearOffsets = mutableListOf<Int>()
    val requestedMonths = mutableListOf<YearMonth>()

    override fun observeCalendar(
        tenantId: String,
        yearOffset: Int,
    ): Flow<Synced<TenantCalendar, TenantError>> {
        requestedYearOffsets += yearOffset
        return calendar
    }

    override fun observeMonthDetail(
        tenantId: String,
        month: YearMonth,
    ): Flow<MonthDetail?> {
        requestedMonths += month
        return monthDetail
    }
}
