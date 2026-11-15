package net.neednot.renttracker.feature.tenants.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

sealed interface PendingChangeUiState {
    data class Balance(
        val was: UiText,
        val now: UiText,
    ) : PendingChangeUiState

    data class NewEntry(
        val parts: List<UiText>,
    ) : PendingChangeUiState
}
