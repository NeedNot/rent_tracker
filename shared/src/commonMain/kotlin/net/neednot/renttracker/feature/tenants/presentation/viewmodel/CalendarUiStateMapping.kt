package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.data.CalendarMonthState
import net.neednot.renttracker.core.designsystem.format.formatMoney
import net.neednot.renttracker.core.designsystem.format.formatMonthDay
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.tenants.domain.CalendarMonth
import net.neednot.renttracker.feature.tenants.domain.TenantCalendar
import net.neednot.renttracker.feature.tenants.domain.TenantError
import net.neednot.renttracker.feature.tenants.presentation.uistate.CalendarSquareUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.CalendarUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.calendar_description_due_not_late
import net.neednot.renttracker.resources.calendar_description_fee_unpaid
import net.neednot.renttracker.resources.calendar_fee_marker
import net.neednot.renttracker.resources.calendar_square_due
import net.neednot.renttracker.resources.calendar_square_owed

private const val MONTHS_IN_YEAR = 12

internal fun Synced<TenantCalendar, TenantError>.toCalendarUiState(
    firstYear: Int,
    currentMonth: YearMonth,
): CalendarUiState {
    val monthsByName = value.months.associateBy { it.month }
    val failure = status as? SyncStatus.Failed
    return CalendarUiState(
        year = value.year.toString(),
        hasPreviousYear = value.year > firstYear,
        squares =
            (1..MONTHS_IN_YEAR).map { number ->
                val month = YearMonth(value.year, number)
                monthsByName[month].toSquare(month, isCurrentMonth = month == currentMonth)
            },
        error = if (value.months.isEmpty()) failure?.error?.text() else null,
    )
}

private fun CalendarMonth?.toSquare(
    month: YearMonth,
    isCurrentMonth: Boolean,
): CalendarSquareUiState {
    val state = this?.state ?: CalendarMonthState.NotATenant
    val amount = this?.amountText()
    val feeAmount =
        this
            ?.takeIf {
                it.hasUnpaidLateFee
            }?.let { it.lateFeeUnpaidCents ?: it.lateFeeCents }
            ?.let(::formatMoney)
    val stateLabel = UiText.Resource(state.label())
    return CalendarSquareUiState(
        month = month.toString(),
        monthName = shortMonthName(month),
        state = state.toCalendarState(),
        stateLabel = stateLabel,
        amount = amount,
        feeMarker = feeAmount?.let { UiText.Resource(Res.string.calendar_fee_marker, listOf(it)) },
        description =
            listOfNotNull(
                UiText.Plain(fullMonthName(month)),
                state.spokenLabel(),
                amount,
                feeAmount?.let { UiText.Resource(Res.string.calendar_description_fee_unpaid, listOf(it)) },
            ),
        isTappable = state != CalendarMonthState.NotATenant,
        isCurrentMonth = isCurrentMonth,
    )
}

private fun CalendarMonth.amountText(): UiText? {
    val due = dueDate
    return when {
        state == CalendarMonthState.Partial || state == CalendarMonthState.Owed -> {
            UiText.Resource(Res.string.calendar_square_owed, listOf(formatMoney(owedCents)))
        }

        state == CalendarMonthState.DueNotLate && due != null -> {
            UiText.Resource(Res.string.calendar_square_due, listOf(formatMonthDay(due)))
        }

        else -> {
            null
        }
    }
}

private fun CalendarMonthState.spokenLabel(): UiText =
    if (this == CalendarMonthState.DueNotLate) {
        UiText.Resource(Res.string.calendar_description_due_not_late)
    } else {
        UiText.Resource(label())
    }
