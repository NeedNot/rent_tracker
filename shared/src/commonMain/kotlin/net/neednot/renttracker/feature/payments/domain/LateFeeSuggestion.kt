package net.neednot.renttracker.feature.payments.domain

import kotlinx.datetime.YearMonth

data class LateFeeSuggestion(
    val forMonth: YearMonth,
    val amountCents: Long,
)
