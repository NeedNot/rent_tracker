package net.neednot.renttracker.feature.debts.presentation.viewmodel

import kotlinx.datetime.YearMonth

data class ChargeSuggestion(
    val forMonth: YearMonth,
    val amountCents: Long,
)
