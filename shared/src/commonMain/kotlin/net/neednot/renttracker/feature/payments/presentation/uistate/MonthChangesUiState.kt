package net.neednot.renttracker.feature.payments.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

sealed interface MonthChangesUiState {
    data class Changes(
        val heading: UiText,
        val lines: List<MonthChangeLine>,
    ) : MonthChangesUiState

    data object CheckFailed : MonthChangesUiState
}

data class MonthChangeLine(
    val month: String,
    val before: UiText,
    val after: UiText,
)
