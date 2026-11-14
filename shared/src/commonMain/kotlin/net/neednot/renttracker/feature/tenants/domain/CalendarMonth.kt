package net.neednot.renttracker.feature.tenants.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.data.CalendarMonthState

data class CalendarMonth(
    val month: YearMonth,
    val state: CalendarMonthState,
    val rentCents: Long,
    val coveredCents: Long,
    val owedCents: Long,
    val hasUnpaidLateFee: Boolean,
    val dueDate: LocalDate?,
    val graceEnd: LocalDate?,
    val wasLate: Boolean,
    val paidByGraceEndCents: Long?,
    val lateFeeCents: Long?,
    val lateFeeUnpaidCents: Long?,
    val suggestedLateFeeCents: Long?,
)
