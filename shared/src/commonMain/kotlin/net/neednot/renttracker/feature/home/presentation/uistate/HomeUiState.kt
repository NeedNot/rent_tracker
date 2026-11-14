package net.neednot.renttracker.feature.home.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data object Empty : HomeUiState

    data class Content(
        val groups: List<GroupCardUiState>,
        val syncError: UiText? = null,
    ) : HomeUiState

    data class Error(
        val message: UiText,
    ) : HomeUiState
}
