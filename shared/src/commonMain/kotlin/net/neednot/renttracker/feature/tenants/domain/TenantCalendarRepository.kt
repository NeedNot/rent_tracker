package net.neednot.renttracker.feature.tenants.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.sync.Synced

interface TenantCalendarRepository {
    fun observeCalendar(
        tenantId: String,
        yearOffset: Int,
    ): Flow<Synced<TenantCalendar, TenantError>>

    fun observeMonthDetail(
        tenantId: String,
        month: YearMonth,
    ): Flow<MonthDetail?>
}
