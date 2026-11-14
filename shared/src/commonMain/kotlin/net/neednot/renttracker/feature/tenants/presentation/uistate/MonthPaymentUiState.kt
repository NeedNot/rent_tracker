package net.neednot.renttracker.feature.tenants.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

data class MonthPaymentUiState(
    val title: UiText,
    val amount: String,
    val notes: List<UiText>,
)
