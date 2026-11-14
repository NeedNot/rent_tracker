package net.neednot.renttracker.core.designsystem.format

import kotlinx.datetime.YearMonth
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char

private val UsMonthYearFormat =
    YearMonth.Format {
        monthName(MonthNames.ENGLISH_ABBREVIATED)
        char(' ')
        year()
    }

fun formatMonth(month: YearMonth): String = UsMonthYearFormat.format(month)
