package net.neednot.renttracker.feature.tenants.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.data.RentTrackerDatabase
import net.neednot.renttracker.core.data.TenantStatus
import net.neednot.renttracker.feature.tenants.domain.MonthDetail

@OptIn(ExperimentalCoroutinesApi::class)
internal fun RentTrackerDatabase.observeMonthDetail(
    tenantId: String,
    month: YearMonth,
): Flow<MonthDetail?> =
    tenantDao().observeById(tenantId).flatMapLatest { tenant ->
        if (tenant == null) return@flatMapLatest flowOf(null)
        combine(
            calendarMonthDao().observeMonth(tenantId, month.toString()),
            allocationDao().observeByTenantId(tenantId),
            observeLedgerSources(tenantId, tenant.groupId),
            groupDao().observeById(tenant.groupId),
        ) { calendarMonth, allocations, ledger, group ->
            if (calendarMonth == null || group == null) {
                null
            } else {
                MonthDetailSources(
                    calendarMonth = calendarMonth,
                    allocations = allocations,
                    ledger = ledger,
                    group = group,
                    isArchived = tenant.status == TenantStatus.Archived,
                ).toMonthDetail()
            }
        }
    }
