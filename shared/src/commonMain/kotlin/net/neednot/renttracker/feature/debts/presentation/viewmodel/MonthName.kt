package net.neednot.renttracker.feature.debts.presentation.viewmodel

import kotlinx.datetime.YearMonth
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.number

internal fun YearMonth.monthName(): String = MonthNames.ENGLISH_FULL.names[month.number - 1]
