package net.neednot.renttracker.feature.debts.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.toLocalDateTime
import net.neednot.renttracker.core.data.DebtEntity
import net.neednot.renttracker.core.data.DebtKind
import net.neednot.renttracker.core.data.GroupEntity
import net.neednot.renttracker.core.data.RentTrackerDatabase
import net.neednot.renttracker.core.data.TenantEntity
import net.neednot.renttracker.core.data.TenantSummaryEntity
import net.neednot.renttracker.feature.debts.domain.ChargeDraft
import net.neednot.renttracker.feature.debts.domain.ChargeEntryOverview
import net.neednot.renttracker.feature.debts.domain.DebtBalance
import net.neednot.renttracker.feature.debts.domain.SavedCharge
import kotlin.time.Instant

private data class GroupToday(
    val group: GroupEntity,
    val today: LocalDate,
)

@OptIn(ExperimentalCoroutinesApi::class)
internal fun RentTrackerDatabase.observeChargeEntry(
    tenantId: String,
    debtId: String?,
    asOf: Flow<Long>,
): Flow<ChargeEntryOverview?> =
    tenantDao().observeById(tenantId).flatMapLatest { tenant ->
        if (tenant == null) flowOf(null) else observeChargeEntry(tenant, debtId, asOf)
    }

private fun RentTrackerDatabase.observeChargeEntry(
    tenant: TenantEntity,
    debtId: String?,
    asOf: Flow<Long>,
): Flow<ChargeEntryOverview?> =
    combine(
        observeGroupToday(tenant.groupId, asOf),
        tenantSummaryDao().observeByTenantId(tenant.id),
        observeCallerAccountId(tenant.groupId),
        observeSavedCharge(debtId),
    ) { groupToday, summary, callerAccountId, debt ->
        groupToday?.let {
            ChargeEntryOverview(
                tenantName = tenant.name,
                groupName = groupToday.group.name,
                balance = summary?.toDebtBalance(),
                today = groupToday.today,
                callerAccountId = callerAccountId,
                charge = debt?.toSavedCharge(),
            )
        }
    }

private fun RentTrackerDatabase.observeGroupToday(
    groupId: String,
    asOf: Flow<Long>,
): Flow<GroupToday?> =
    combine(groupDao().observeById(groupId), asOf) { group, asOfMillis ->
        group?.let { GroupToday(group, group.localDateAt(asOfMillis)) }
    }

private fun RentTrackerDatabase.observeCallerAccountId(groupId: String): Flow<String?> =
    membershipDao().observeByGroupId(groupId).map { it?.accountId }

private fun RentTrackerDatabase.observeSavedCharge(debtId: String?): Flow<DebtEntity?> =
    if (debtId == null) {
        flowOf(null)
    } else {
        debtDao().observeById(debtId).map { debt -> debt?.takeIf { it.deletedAt == null } }
    }

private fun GroupEntity.localDateAt(epochMillis: Long): LocalDate =
    Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.of(timeZone)).date

private fun TenantSummaryEntity.toDebtBalance(): DebtBalance =
    DebtBalance(owedCents = rentOwedCents + lateFeesOwedCents + otherOwedCents, creditCents = creditCents)

private fun DebtEntity.toSavedCharge(): SavedCharge? {
    val chargeDate = LocalDate.parse(date)
    val draft =
        when (kind) {
            DebtKind.LateFee -> {
                forMonth?.let { ChargeDraft.LateFee(amountCents, chargeDate, YearMonth.parse(it), note) }
            }

            DebtKind.Custom -> {
                note?.let { ChargeDraft.Custom(amountCents, chargeDate, it) }
            }
        }
    return draft?.let { SavedCharge(debtId = id, version = version, draft = it) }
}
