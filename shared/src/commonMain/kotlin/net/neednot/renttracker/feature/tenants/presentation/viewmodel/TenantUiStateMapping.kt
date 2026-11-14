package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import kotlinx.datetime.TimeZone
import kotlinx.datetime.minusMonth
import kotlinx.datetime.yearMonth
import net.neednot.renttracker.core.designsystem.format.formatDate
import net.neednot.renttracker.core.designsystem.format.formatMoney
import net.neednot.renttracker.core.designsystem.format.formatMonth
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.tenants.domain.LateFeeSuggestion
import net.neednot.renttracker.feature.tenants.domain.OwedBreakdown
import net.neednot.renttracker.feature.tenants.domain.RentTerm
import net.neednot.renttracker.feature.tenants.domain.TenantBalance
import net.neednot.renttracker.feature.tenants.domain.TenantError
import net.neednot.renttracker.feature.tenants.domain.TenantOverview
import net.neednot.renttracker.feature.tenants.presentation.uistate.BalanceLineUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.LateFeeSuggestionUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.RentTermRowUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantBalanceUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantDetailsUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantStatusChangeUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ledger_line_late_fees
import net.neednot.renttracker.resources.ledger_line_other
import net.neednot.renttracker.resources.ledger_line_rent
import net.neednot.renttracker.resources.ledger_suggestion_message
import net.neednot.renttracker.resources.tenant_error_not_found
import net.neednot.renttracker.resources.tenant_grace_days
import net.neednot.renttracker.resources.tenant_grace_days_group
import net.neednot.renttracker.resources.tenant_late_fee_group
import net.neednot.renttracker.resources.tenant_late_fee_percent_of_rent
import net.neednot.renttracker.resources.tenant_next_due
import net.neednot.renttracker.resources.tenant_partial_move_out_warning
import net.neednot.renttracker.resources.tenant_rent_term_from
import net.neednot.renttracker.resources.tenant_rent_term_range
import net.neednot.renttracker.resources.tenant_rent_term_summary

private const val LEDGER_PREVIEW_SIZE = 3

internal fun Synced<TenantOverview?, TenantError>.toTenantUiState(
    statusChange: TenantStatusChangeUiState,
    timeZone: TimeZone,
): TenantUiState {
    val syncError = (status as? SyncStatus.Failed)?.error?.text()
    val overview = value
    return when {
        overview != null -> overview.toContent(syncError, statusChange, timeZone)
        syncError != null -> TenantUiState.Error(syncError)
        status == SyncStatus.Syncing -> TenantUiState.Loading
        else -> TenantUiState.Error(UiText.Resource(Res.string.tenant_error_not_found))
    }
}

private fun TenantOverview.toContent(
    syncError: UiText?,
    statusChange: TenantStatusChangeUiState,
    timeZone: TimeZone,
): TenantUiState.Content =
    TenantUiState.Content(
        name = tenant.details.name,
        groupId = tenant.groupId,
        groupName = group.name,
        isArchived = tenant.isArchived,
        balance = balance?.toUiState(),
        rentTerms = rentTermRows(),
        details = detailsUiState(),
        ledgerPreview = ledger.take(LEDGER_PREVIEW_SIZE).map { it.toRowUiState(timeZone) },
        canLogPayment = !tenant.isArchived && permissions?.canLogPayments == true,
        canAddCharge = !tenant.isArchived && permissions?.canAddCharges == true,
        lateFeeSuggestion = lateFeeSuggestion?.toUiState(),
        syncError = syncError,
        statusChange = statusChange,
    )

private fun TenantBalance.toUiState(): TenantBalanceUiState =
    TenantBalanceUiState(
        state = state,
        total = total.cardText(),
        lines = owedBreakdown.lines(),
        nextDue = nextDueDate?.let { UiText.Resource(Res.string.tenant_next_due, listOf(formatDate(it))) },
        partialMoveOutWarning =
            partialMoveOutPeriod?.let { period ->
                UiText.Resource(Res.string.tenant_partial_move_out_warning, listOf(formatDayRange(period)))
            },
    )

private fun OwedBreakdown.lines(): List<BalanceLineUiState> =
    listOf(
        Res.string.ledger_line_rent to rentCents,
        Res.string.ledger_line_late_fees to lateFeesCents,
        Res.string.ledger_line_other to otherCents,
    ).filter { (_, cents) -> cents > 0 }
        .map { (label, cents) -> BalanceLineUiState(UiText.Resource(label), formatMoney(cents)) }

private fun LateFeeSuggestion.toUiState(): LateFeeSuggestionUiState =
    LateFeeSuggestionUiState(
        message =
            UiText.Resource(Res.string.ledger_suggestion_message, listOf(fullMonthName(month), formatMoney(cents))),
        month = month.toString(),
        cents = cents,
    )

private fun TenantOverview.rentTermRows(): List<RentTermRowUiState> {
    val currentTerm = activeRentTerms.lastOrNull { it.startMonth <= today.yearMonth }
    return activeRentTerms
        .mapIndexed { index, term ->
            RentTermRowUiState(
                rentTermId = term.rentTermId,
                summary =
                    UiText.Resource(
                        Res.string.tenant_rent_term_summary,
                        listOf(formatMoney(term.amountCents), ordinal(term.dueDay)),
                    ),
                period = term.period(replacedBy = activeRentTerms.getOrNull(index + 1)),
                isCurrent = term == currentTerm,
            )
        }.reversed()
}

private fun RentTerm.period(replacedBy: RentTerm?): UiText =
    if (replacedBy == null) {
        UiText.Resource(Res.string.tenant_rent_term_from, listOf(formatMonth(startMonth)))
    } else {
        UiText.Resource(
            Res.string.tenant_rent_term_range,
            listOf(formatMonth(startMonth), formatMonth(replacedBy.startMonth.minusMonth())),
        )
    }

private fun TenantOverview.detailsUiState(): TenantDetailsUiState =
    TenantDetailsUiState(
        contactName = tenant.details.contactName,
        phone = tenant.details.contactPhone,
        email = tenant.details.contactEmail,
        movedIn = formatDate(tenant.details.moveIn),
        lastDayInUnit = tenant.details.moveOut?.let(::formatDate),
        securityDeposit = tenant.details.depositCents?.let(::formatMoney),
        gracePeriod = gracePeriodText(),
        lateFee = lateFeeText(),
        notes = tenant.details.notes,
    )

private fun TenantOverview.gracePeriodText(): UiText {
    val override = tenant.details.graceDaysOverride
    return if (override != null) {
        UiText.Plural(Res.plurals.tenant_grace_days, override, listOf(override))
    } else {
        UiText.Plural(Res.plurals.tenant_grace_days_group, group.graceDays, listOf(group.graceDays))
    }
}

private fun TenantOverview.lateFeeText(): UiText {
    val override = tenant.details.lateFeeFlatOverrideCents
    val groupFee = group.lateFeeFlatCents
    return when {
        override != null -> UiText.Plain(formatMoney(override))
        groupFee != null -> UiText.Resource(Res.string.tenant_late_fee_group, listOf(formatMoney(groupFee)))
        else -> UiText.Resource(Res.string.tenant_late_fee_percent_of_rent)
    }
}
