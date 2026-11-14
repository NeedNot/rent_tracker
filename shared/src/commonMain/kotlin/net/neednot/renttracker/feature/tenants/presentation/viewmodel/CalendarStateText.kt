package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import kotlinx.datetime.YearMonth
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import net.neednot.renttracker.core.data.CalendarMonthState
import net.neednot.renttracker.core.designsystem.components.CalendarState
import net.neednot.renttracker.core.designsystem.components.label
import org.jetbrains.compose.resources.StringResource

private val ShortMonthNameFormat = YearMonth.Format { monthName(MonthNames.ENGLISH_ABBREVIATED) }

internal fun shortMonthName(month: YearMonth): String = month.format(ShortMonthNameFormat)

internal fun CalendarMonthState.label(): StringResource = toCalendarState().label()

internal fun CalendarMonthState.toCalendarState(): CalendarState =
    when (this) {
        CalendarMonthState.Paid -> CalendarState.Paid
        CalendarMonthState.Partial -> CalendarState.Partial
        CalendarMonthState.Owed -> CalendarState.Owed
        CalendarMonthState.DueNotLate -> CalendarState.DueNotLate
        CalendarMonthState.Upcoming -> CalendarState.Upcoming
        CalendarMonthState.Prepaid -> CalendarState.Prepaid
        CalendarMonthState.NotATenant -> CalendarState.NotATenant
    }
