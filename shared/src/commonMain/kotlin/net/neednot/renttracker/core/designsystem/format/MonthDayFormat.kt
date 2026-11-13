package net.neednot.renttracker.core.designsystem.format

import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char

private val UsMonthDayFormat =
    LocalDate.Format {
        monthName(MonthNames.ENGLISH_ABBREVIATED)
        char(' ')
        day(padding = Padding.NONE)
    }

fun formatMonthDay(date: LocalDate): String = UsMonthDayFormat.format(date)
