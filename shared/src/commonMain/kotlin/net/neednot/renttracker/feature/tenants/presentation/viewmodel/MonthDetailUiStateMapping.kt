package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import net.neednot.renttracker.core.data.CalendarMonthState
import net.neednot.renttracker.core.data.PaymentOrder
import net.neednot.renttracker.core.designsystem.format.formatMoney
import net.neednot.renttracker.core.designsystem.format.formatMonthDay
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.tenants.domain.CalendarMonth
import net.neednot.renttracker.feature.tenants.domain.MonthDetail
import net.neednot.renttracker.feature.tenants.domain.MonthLateFee
import net.neednot.renttracker.feature.tenants.presentation.uistate.AddFeeUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.BalanceLineUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.MonthDetailUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.MonthLateFeeUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.MonthLateNoticeUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.calendar_add_fee
import net.neednot.renttracker.resources.calendar_fee_added_named
import net.neednot.renttracker.resources.calendar_fee_added_you
import net.neednot.renttracker.resources.calendar_fee_rent_first
import net.neednot.renttracker.resources.calendar_fee_title_paid
import net.neednot.renttracker.resources.calendar_fee_title_unpaid
import net.neednot.renttracker.resources.calendar_late_generic
import net.neednot.renttracker.resources.calendar_late_paid_none
import net.neednot.renttracker.resources.calendar_late_paid_some
import net.neednot.renttracker.resources.calendar_late_title
import net.neednot.renttracker.resources.calendar_paid_note
import net.neednot.renttracker.resources.calendar_prepaid_note
import net.neednot.renttracker.resources.calendar_rent
import net.neednot.renttracker.resources.calendar_rent_due
import net.neednot.renttracker.resources.calendar_total_covered
import net.neednot.renttracker.resources.calendar_total_owed
import net.neednot.renttracker.resources.calendar_total_owed_on_rent
import net.neednot.renttracker.resources.ledger_may_not_apply

internal fun MonthDetail.toUiState(): MonthDetailUiState =
    MonthDetailUiState(
        title = fullMonthAndYear(month.month),
        state = month.state.toCalendarState(),
        stateLabel = UiText.Resource(month.state.label()),
        rentLabel = month.rentLabel(),
        rent = formatMoney(month.rentCents),
        payments = payments.map { it.toUiState(month) },
        coverageNote = month.coverageNote(),
        totals = totals(),
        lateNotice = lateNotice(),
        lateFee = lateFee?.toUiState(month, paymentOrder),
    )

private fun CalendarMonth.rentLabel(): UiText =
    dueDate
        ?.let { UiText.Resource(Res.string.calendar_rent_due, listOf(formatMonthDay(it))) }
        ?: UiText.Resource(Res.string.calendar_rent)

private fun CalendarMonth.coverageNote(): UiText? {
    if (coveredCents == 0L) return null
    return when (state) {
        CalendarMonthState.Upcoming, CalendarMonthState.Prepaid -> {
            UiText.Resource(Res.string.calendar_prepaid_note, listOf(formatMoney(coveredCents)))
        }

        CalendarMonthState.DueNotLate -> {
            UiText.Resource(Res.string.calendar_paid_note, listOf(formatMoney(coveredCents)))
        }

        else -> {
            null
        }
    }
}

private fun MonthDetail.totals(): List<BalanceLineUiState> {
    if (month.state !in StatesWithTotals) return emptyList()
    val owedLabel = if (lateFee == null) Res.string.calendar_total_owed else Res.string.calendar_total_owed_on_rent
    return listOf(
        BalanceLineUiState(UiText.Resource(Res.string.calendar_total_covered), formatMoney(month.coveredCents)),
        BalanceLineUiState(UiText.Resource(owedLabel), formatMoney(month.owedCents)),
    )
}

private val StatesWithTotals = setOf(CalendarMonthState.Paid, CalendarMonthState.Partial, CalendarMonthState.Owed)

private fun MonthDetail.lateNotice(): MonthLateNoticeUiState? {
    if (!month.wasLate || lateFee != null) return null
    return MonthLateNoticeUiState(
        title = UiText.Resource(Res.string.calendar_late_title),
        message = month.lateMessage(),
        addFee = if (canAddCharge) AddFeeUiState(month.month.toString(), month.suggestedLateFeeCents) else null,
    )
}

private fun CalendarMonth.lateMessage(): UiText {
    val graceEnd = graceEnd?.let(::formatMonthDay)
    val paid = paidByGraceEndCents
    return when {
        graceEnd == null || paid == null -> UiText.Resource(Res.string.calendar_late_generic)
        paid == 0L -> UiText.Resource(Res.string.calendar_late_paid_none, listOf(graceEnd))
        else -> UiText.Resource(Res.string.calendar_late_paid_some, listOf(formatMoney(paid), graceEnd))
    }
}

private fun MonthLateFee.toUiState(
    month: CalendarMonth,
    paymentOrder: PaymentOrder,
): MonthLateFeeUiState {
    val monthName = fullMonthName(month.month)
    val title = if (isUnpaid) Res.string.calendar_fee_title_unpaid else Res.string.calendar_fee_title_paid
    val rentFirstNote =
        if (isUnpaid &&
            paymentOrder == PaymentOrder.RentFirst
        ) {
            Res.string.calendar_fee_rent_first
        } else {
            null
        }
    return MonthLateFeeUiState(
        title = UiText.Resource(title, listOf(monthName)),
        amount = formatMoney(amountCents),
        notes =
            listOfNotNull(
                addedNote(),
                rentFirstNote?.let { UiText.Resource(it) },
                if (mayNotApply) UiText.Resource(Res.string.ledger_may_not_apply) else null,
            ),
        isUnpaid = isUnpaid,
    )
}

private fun MonthLateFee.addedNote(): UiText =
    addedBy.text(
        you = Res.string.calendar_fee_added_you,
        named = Res.string.calendar_fee_added_named,
        formatMonthDay(addedOn),
    )
