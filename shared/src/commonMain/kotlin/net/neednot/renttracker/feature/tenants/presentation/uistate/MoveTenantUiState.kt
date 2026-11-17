package net.neednot.renttracker.feature.tenants.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

sealed interface MoveTenantUiState {
    data object Loading : MoveTenantUiState

    data object Empty : MoveTenantUiState

    data class Content(
        val tenantName: String,
        val targets: List<MoveTargetUiState>,
        val contactName: String? = null,
        val balance: UiText? = null,
        val confirmation: MoveConfirmationUiState? = null,
        val isMoving: Boolean = false,
    ) : MoveTenantUiState

    data class Error(
        val message: UiText,
    ) : MoveTenantUiState
}
