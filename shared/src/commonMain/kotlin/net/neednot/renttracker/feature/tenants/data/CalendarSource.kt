package net.neednot.renttracker.feature.tenants.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.neednot.renttracker.core.data.RentTrackerDatabase
import net.neednot.renttracker.feature.tenants.domain.TenantCalendar

internal fun RentTrackerDatabase.observeCachedCalendar(
    tenantId: String,
    year: Int,
): Flow<TenantCalendar> =
    calendarMonthDao().observeYear(tenantId, year).map { months ->
        TenantCalendar(year = year, months = months.map { it.toCalendarMonth() })
    }
