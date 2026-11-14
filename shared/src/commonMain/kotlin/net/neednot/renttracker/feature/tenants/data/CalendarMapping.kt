package net.neednot.renttracker.feature.tenants.data

import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.data.CalendarMonthEntity
import net.neednot.renttracker.feature.tenants.domain.CalendarMonth

internal fun CalendarMonthEntity.toCalendarMonth(): CalendarMonth =
    CalendarMonth(
        month = YearMonth.parse(month),
        state = state,
        rentCents = rentCents,
        coveredCents = coveredCents,
        owedCents = owedCents,
        hasUnpaidLateFee = hasUnpaidLateFee,
        dueDate = dueDate?.let(LocalDate::parse),
        graceEnd = graceEnd?.let(LocalDate::parse),
        wasLate = wasLate,
        paidByGraceEndCents = paidByGraceEndCents,
        lateFeeCents = lateFeeCents,
        lateFeeUnpaidCents = lateFeeUnpaidCents,
        suggestedLateFeeCents = suggestedLateFeeCents,
    )
