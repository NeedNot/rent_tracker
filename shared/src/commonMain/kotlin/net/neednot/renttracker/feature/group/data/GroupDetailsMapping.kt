package net.neednot.renttracker.feature.group.data

import kotlinx.datetime.LocalDate
import net.neednot.renttracker.core.access.CallerAccess
import net.neednot.renttracker.core.data.GroupEntity
import net.neednot.renttracker.core.data.GroupSummaryEntity
import net.neednot.renttracker.core.data.TenantBalanceState
import net.neednot.renttracker.core.data.TenantEntity
import net.neednot.renttracker.core.data.TenantSummaryEntity
import net.neednot.renttracker.feature.group.domain.GroupDetails
import net.neednot.renttracker.feature.group.domain.GroupSettings
import net.neednot.renttracker.feature.group.domain.GroupTenant
import net.neednot.renttracker.feature.group.domain.GroupTenantState
import net.neednot.renttracker.feature.group.domain.TenantBalance
import kotlin.time.Instant

internal fun GroupEntity.toGroupDetails(
    summary: GroupSummaryEntity,
    activeTenants: List<GroupTenant>,
    archivedTenants: List<GroupTenant>,
    access: CallerAccess,
): GroupDetails =
    GroupDetails(
        groupId = id,
        name = name,
        settings =
            GroupSettings(
                name = name,
                timeZone = timeZone,
                paymentOrder = paymentOrder,
                graceDays = graceDays.toInt(),
                lateFeeFlatCents = lateFeeFlatCents,
            ),
        version = version,
        archivedAt = archivedAt?.let(Instant::fromEpochMilliseconds),
        totalOwedCents = summary.totalOwedCents,
        lateTenantCount = summary.lateTenantCount.toInt(),
        archivedOwedCents = summary.archivedOwedCents,
        activeTenants = activeTenants.sortedBy { it.state },
        archivedTenants = archivedTenants,
        access = access,
    )

internal fun List<TenantEntity>.toGroupTenants(summaries: List<TenantSummaryEntity>): List<GroupTenant> {
    val summaryByTenantId = summaries.associateBy { it.tenantId }
    return mapNotNull { tenant -> summaryByTenantId[tenant.id]?.let { summary -> tenant.toGroupTenant(summary) } }
}

private fun TenantEntity.toGroupTenant(summary: TenantSummaryEntity): GroupTenant =
    GroupTenant(
        tenantId = id,
        name = name,
        contactName = contactName,
        nextDueDate = summary.nextDueDate?.let(LocalDate::parse),
        state = summary.state.toGroupTenantState(),
        balance = summary.balance(),
    )

private fun TenantSummaryEntity.balance(): TenantBalance {
    val owedCents = rentOwedCents + lateFeesOwedCents + otherOwedCents
    return when {
        owedCents > 0 -> TenantBalance.Owes(owedCents)
        creditCents > 0 -> TenantBalance.Credit(creditCents)
        else -> TenantBalance.PaidUp
    }
}

private fun TenantBalanceState.toGroupTenantState(): GroupTenantState =
    when (this) {
        TenantBalanceState.Late -> GroupTenantState.Late
        TenantBalanceState.Owes -> GroupTenantState.Owes
        TenantBalanceState.PaidUp -> GroupTenantState.PaidUp
    }
