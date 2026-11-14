package net.neednot.renttracker.feature.payments.presentation.uistate

import kotlinx.datetime.YearMonth

data class LateFeeOfferUiState(
    val month: YearMonth,
    val isChecked: Boolean,
    val amount: String,
)
