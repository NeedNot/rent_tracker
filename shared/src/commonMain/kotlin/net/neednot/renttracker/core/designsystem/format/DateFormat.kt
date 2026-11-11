package net.neednot.renttracker.core.designsystem.format

import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char

private val UsDateFormat =
    LocalDate.Format {
        monthName(MonthNames.ENGLISH_ABBREVIATED)
        char(' ')
        day(padding = Padding.NONE)
        chars(", ")
        year()
    }

fun formatDate(date: LocalDate): String = UsDateFormat.format(date)

fun formatDate(isoDate: String): String = formatDate(LocalDate.parse(isoDate))
