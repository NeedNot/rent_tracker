package net.neednot.renttracker.feature.debts.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

data class DuplicateLateFeeUiState(
    val date: String,
    val by: UiText,
    val amount: String,
)
