package net.neednot.renttracker.feature.payments.presentation.viewmodel

import net.neednot.renttracker.core.designsystem.components.CalendarState
import net.neednot.renttracker.core.designsystem.components.label
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.payments.domain.MonthState
import net.neednot.renttracker.feature.payments.domain.MonthStateChange
import net.neednot.renttracker.feature.payments.presentation.uistate.MonthChangeLine
import net.neednot.renttracker.feature.payments.presentation.uistate.MonthChangesUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.payment_preview_heading

internal fun List<MonthStateChange>.toUiState(): MonthChangesUiState.Changes? =
    takeIf { it.isNotEmpty() }?.let { changes ->
        MonthChangesUiState.Changes(
            heading = UiText.Plural(Res.plurals.payment_preview_heading, changes.size, listOf(changes.size)),
            lines = changes.map { it.line() },
        )
    }

private fun MonthStateChange.line(): MonthChangeLine =
    MonthChangeLine("${month.monthName()} ${month.year}", before.name(), after.name())

private fun MonthState.name(): UiText = UiText.Resource(toCalendarState().label())

private fun MonthState.toCalendarState(): CalendarState =
    when (this) {
        MonthState.Paid -> CalendarState.Paid
        MonthState.Partial -> CalendarState.Partial
        MonthState.Owed -> CalendarState.Owed
        MonthState.DueNotLate -> CalendarState.DueNotLate
        MonthState.Upcoming -> CalendarState.Upcoming
        MonthState.Prepaid -> CalendarState.Prepaid
        MonthState.NotATenant -> CalendarState.NotATenant
    }
