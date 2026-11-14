package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import net.neednot.renttracker.core.designsystem.format.creditText
import net.neednot.renttracker.core.designsystem.format.owesText
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.tenants.domain.LedgerBalance
import net.neednot.renttracker.feature.tenants.domain.Person
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.balance_paid_up
import org.jetbrains.compose.resources.StringResource
import kotlin.time.Instant

private val MonthNameFormat = YearMonth.Format { monthName(MonthNames.ENGLISH_FULL) }

private val MonthAndYearFormat =
    YearMonth.Format {
        monthName(MonthNames.ENGLISH_FULL)
        char(' ')
        year()
    }

internal fun fullMonthName(month: YearMonth): String = month.format(MonthNameFormat)

internal fun fullMonthAndYear(month: YearMonth): String = month.format(MonthAndYearFormat)

internal fun Instant.localDate(timeZone: TimeZone): LocalDate = toLocalDateTime(timeZone).date

internal fun LedgerBalance.inWords(): UiText =
    when (this) {
        is LedgerBalance.Owed -> owesText(cents)
        is LedgerBalance.Credit -> creditText(cents)
        LedgerBalance.PaidUp -> UiText.Resource(Res.string.balance_paid_up)
    }

internal fun Person.text(
    you: StringResource,
    named: StringResource,
    vararg leadingArgs: Any,
): UiText =
    when (this) {
        Person.You -> UiText.Resource(you, leadingArgs.toList())
        is Person.Named -> UiText.Resource(named, leadingArgs.toList() + name)
    }
