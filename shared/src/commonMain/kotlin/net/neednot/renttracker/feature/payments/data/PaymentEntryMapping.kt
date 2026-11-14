package net.neednot.renttracker.feature.payments.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.data.AccessLevel
import net.neednot.renttracker.core.data.MembershipEntity
import net.neednot.renttracker.core.data.MembershipRole
import net.neednot.renttracker.core.data.PaymentEntity
import net.neednot.renttracker.core.data.RentTrackerDatabase
import net.neednot.renttracker.core.data.TenantEntity
import net.neednot.renttracker.core.data.TenantSummaryEntity
import net.neednot.renttracker.feature.payments.domain.LateFeeSuggestion
import net.neednot.renttracker.feature.payments.domain.PaymentBalance
import net.neednot.renttracker.feature.payments.domain.PaymentDraft
import net.neednot.renttracker.feature.payments.domain.PaymentEntryOverview
import net.neednot.renttracker.feature.payments.domain.SavedPayment

@OptIn(ExperimentalCoroutinesApi::class)
internal fun RentTrackerDatabase.observePaymentEntry(
    tenantId: String,
    paymentId: String?,
    asOf: Flow<Long>,
): Flow<PaymentEntryOverview?> =
    tenantDao().observeById(tenantId).flatMapLatest { tenant ->
        if (tenant == null) flowOf(null) else observePaymentEntry(tenant, paymentId, asOf)
    }

private fun RentTrackerDatabase.observePaymentEntry(
    tenant: TenantEntity,
    paymentId: String?,
    asOf: Flow<Long>,
): Flow<PaymentEntryOverview?> =
    combine(
        observeGroupToday(tenant.groupId, asOf),
        tenantSummaryDao().observeByTenantId(tenant.id),
        observeCaller(tenant.groupId),
        dismissedLateFeeSuggestionDao().observeDismissedMonths(tenant.id),
        observeSavedPayment(paymentId),
    ) { groupToday, summary, caller, dismissedMonths, payment ->
        groupToday?.let {
            PaymentEntryOverview(
                tenantName = tenant.name,
                groupName = groupToday.group.name,
                balance = summary?.toPaymentBalance(),
                today = groupToday.today,
                lastPaymentMethod = tenant.lastPaymentMethod,
                lateFeeSuggestion = summary?.toLateFeeSuggestion()?.takeUnless { it.isDismissedIn(dismissedMonths) },
                canAddDebts = caller?.canAddDebts() == true,
                callerAccountId = caller?.accountId,
                payment = payment?.toSavedPayment(),
            )
        }
    }

private fun RentTrackerDatabase.observeSavedPayment(paymentId: String?): Flow<PaymentEntity?> =
    if (paymentId == null) {
        flowOf(null)
    } else {
        paymentDao().observeById(paymentId).map { payment -> payment?.takeIf { it.deletedAt == null } }
    }

private fun TenantSummaryEntity.toPaymentBalance(): PaymentBalance =
    PaymentBalance(owedCents = rentOwedCents + lateFeesOwedCents + otherOwedCents, creditCents = creditCents)

private fun TenantSummaryEntity.toLateFeeSuggestion(): LateFeeSuggestion? {
    val month = lateFeeSuggestionMonth
    val cents = lateFeeSuggestionCents
    return if (month != null && cents != null) LateFeeSuggestion(YearMonth.parse(month), cents) else null
}

private fun LateFeeSuggestion.isDismissedIn(dismissedMonths: List<String>): Boolean =
    forMonth.toString() in dismissedMonths

private fun MembershipEntity.canAddDebts(): Boolean = role == MembershipRole.Owner || debts != AccessLevel.Read

private fun PaymentEntity.toSavedPayment(): SavedPayment =
    SavedPayment(
        paymentId = id,
        version = version,
        details =
            PaymentDraft(
                amountCents = amountCents,
                dateReceived = LocalDate.parse(dateReceived),
                method = method,
                note = note,
            ),
    )
