package net.neednot.renttracker.feature.tenants.domain

import kotlinx.datetime.YearMonth

data class LateFeeSuggestion(
    val month: YearMonth,
    val cents: Long,
)
