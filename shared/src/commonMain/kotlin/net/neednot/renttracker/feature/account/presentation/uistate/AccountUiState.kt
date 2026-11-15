package net.neednot.renttracker.feature.account.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

sealed interface AccountUiState {
    data object Loading : AccountUiState

    data class Content(
        val initials: String,
        val displayName: String,
        val email: String,
        val deletion: DeletionRowUiState,
        val editNameDialog: EditNameDialogUiState? = null,
        val isDeleteDialogShown: Boolean = false,
        val unsyncedPaymentsOnSignOut: Int? = null,
        val syncError: UiText? = null,
    ) : AccountUiState

    data class Error(
        val message: UiText,
    ) : AccountUiState
}
