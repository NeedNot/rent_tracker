package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.yearMonth
import net.neednot.renttracker.core.designsystem.format.creditText
import net.neednot.renttracker.core.designsystem.format.formatMoney
import net.neednot.renttracker.core.designsystem.format.owesText
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.tenants.domain.BalanceTotal
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.balance_paid_up

private const val TENS = 10
private const val HUNDREDS = 100
private val TeenDays = 11..13
private const val FIRST = 1
private const val SECOND = 2
private const val THIRD = 3

private val MonthAndDay =
    LocalDate.Format {
        monthName(MonthNames.ENGLISH_ABBREVIATED)
        char(' ')
        day(padding = Padding.NONE)
    }

internal fun BalanceTotal.inWords(): UiText =
    when {
        owedCents > 0 -> owesText(owedCents)
        creditCents > 0 -> creditText(creditCents)
        else -> UiText.Resource(Res.string.balance_paid_up)
    }

internal fun BalanceTotal.cardText(): UiText =
    when {
        owedCents > 0 -> owesText(owedCents)
        creditCents > 0 -> creditText(creditCents)
        else -> UiText.Plain(formatMoney(0))
    }

internal fun formatDayRange(days: ClosedRange<LocalDate>): String {
    val start = days.start.format(MonthAndDay)
    val isWithinOneMonth = days.start.yearMonth == days.endInclusive.yearMonth
    return if (isWithinOneMonth) {
        "$start–${days.endInclusive.day}"
    } else {
        "$start – ${days.endInclusive.format(
            MonthAndDay,
        )}"
    }
}

internal fun ordinal(day: Int): String {
    val suffix =
        when {
            day % HUNDREDS in TeenDays -> "th"
            day % TENS == FIRST -> "st"
            day % TENS == SECOND -> "nd"
            day % TENS == THIRD -> "rd"
            else -> "th"
        }
    return "$day$suffix"
}
