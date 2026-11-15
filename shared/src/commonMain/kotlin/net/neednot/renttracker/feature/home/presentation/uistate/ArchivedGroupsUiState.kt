package net.neednot.renttracker.feature.home.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

sealed interface ArchivedGroupsUiState {
    data object Loading : ArchivedGroupsUiState

    data object Empty : ArchivedGroupsUiState

    data class Content(
        val groups: List<ArchivedGroupCardUiState>,
        val syncError: UiText? = null,
    ) : ArchivedGroupsUiState

    data class Error(
        val message: UiText,
    ) : ArchivedGroupsUiState
}
