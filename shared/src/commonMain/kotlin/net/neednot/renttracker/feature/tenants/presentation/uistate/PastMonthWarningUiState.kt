package net.neednot.renttracker.feature.tenants.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

sealed interface PastMonthWarningUiState {
    data object CheckingBalance : PastMonthWarningUiState

    data class BalanceChange(
        val before: UiText,
        val after: UiText,
    ) : PastMonthWarningUiState

    data class CheckFailed(
        val message: UiText,
    ) : PastMonthWarningUiState
}
