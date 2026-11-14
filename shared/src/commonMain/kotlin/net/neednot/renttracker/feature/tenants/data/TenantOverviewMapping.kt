package net.neednot.renttracker.feature.tenants.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.toLocalDateTime
import net.neednot.renttracker.core.data.GroupEntity
import net.neednot.renttracker.core.data.RentTermEntity
import net.neednot.renttracker.core.data.RentTrackerDatabase
import net.neednot.renttracker.core.data.TenantEntity
import net.neednot.renttracker.core.data.TenantStatus
import net.neednot.renttracker.core.data.TenantSummaryEntity
import net.neednot.renttracker.feature.tenants.domain.BalanceTotal
import net.neednot.renttracker.feature.tenants.domain.RentTerm
import net.neednot.renttracker.feature.tenants.domain.Tenant
import net.neednot.renttracker.feature.tenants.domain.TenantBalance
import net.neednot.renttracker.feature.tenants.domain.TenantDetails
import net.neednot.renttracker.feature.tenants.domain.TenantGroup
import net.neednot.renttracker.feature.tenants.domain.TenantOverview
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
internal fun RentTrackerDatabase.observeTenantOverview(
    tenantId: String,
    asOf: Flow<Long>,
): Flow<TenantOverview?> =
    tenantDao().observeById(tenantId).flatMapLatest { tenant ->
        if (tenant == null) return@flatMapLatest flowOf(null)
        combine(
            groupDao().observeById(tenant.groupId),
            tenantSummaryDao().observeByTenantId(tenantId),
            rentTermDao().observeByTenantId(tenantId),
            asOf,
        ) { group, summary, rentTerms, asOfMillis ->
            group?.let {
                TenantOverview(
                    tenant = tenant.toTenant(),
                    group = group.toTenantGroup(),
                    balance = summary?.toTenantBalance(),
                    activeRentTerms = rentTerms.filter { it.deletedAt == null }.map { it.toRentTerm() },
                    today = group.localDateAt(asOfMillis),
                )
            }
        }
    }

private fun GroupEntity.localDateAt(epochMillis: Long): LocalDate =
    Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.of(timeZone)).date

private fun TenantEntity.toTenant(): Tenant =
    Tenant(
        tenantId = id,
        groupId = groupId,
        isArchived = status == TenantStatus.Archived,
        version = version,
        details =
            TenantDetails(
                name = name,
                contactName = contactName,
                contactPhone = contactPhone,
                contactEmail = contactEmail,
                notes = notes,
                depositCents = depositCents,
                moveIn = LocalDate.parse(moveIn),
                moveOut = moveOut?.let(LocalDate::parse),
                graceDaysOverride = graceDaysOverride?.toInt(),
                lateFeeFlatOverrideCents = lateFeeFlatOverrideCents,
            ),
    )

private fun GroupEntity.toTenantGroup(): TenantGroup =
    TenantGroup(name = name, graceDays = graceDays.toInt(), lateFeeFlatCents = lateFeeFlatCents)

private fun TenantSummaryEntity.toTenantBalance(): TenantBalance =
    TenantBalance(
        state = state,
        total = BalanceTotal(owedCents = rentOwedCents + lateFeesOwedCents + otherOwedCents, creditCents = creditCents),
        nextDueDate = nextDueDate?.let(LocalDate::parse),
        partialMoveOutPeriod = partialMoveOutPeriod(),
    )

private fun TenantSummaryEntity.partialMoveOutPeriod(): ClosedRange<LocalDate>? {
    val start = partialMoveOutStart
    val end = partialMoveOutEnd
    return if (start != null && end != null) LocalDate.parse(start)..LocalDate.parse(end) else null
}

private fun RentTermEntity.toRentTerm(): RentTerm =
    RentTerm(
        rentTermId = id,
        startMonth = YearMonth.parse(startMonth),
        amountCents = amountCents,
        dueDay = dueDay.toInt(),
        version = version,
    )
