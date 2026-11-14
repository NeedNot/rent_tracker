package net.neednot.renttracker.feature.payments.domain

import kotlinx.datetime.YearMonth

data class MonthStateChange(
    val month: YearMonth,
    val before: MonthState,
    val after: MonthState,
)
