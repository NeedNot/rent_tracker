package net.neednot.renttracker.feature.tenants.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

data class MonthLateFeeUiState(
    val title: UiText,
    val amount: String,
    val notes: List<UiText>,
    val isUnpaid: Boolean,
)
