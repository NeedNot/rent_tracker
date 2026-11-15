package net.neednot.renttracker.feature.account.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

sealed interface DeletionRowUiState {
    data object Available : DeletionRowUiState

    data class Requested(
        val supportingText: UiText,
    ) : DeletionRowUiState
}
