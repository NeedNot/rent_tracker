package net.neednot.renttracker.feature.tenants.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

sealed interface MoveTenantUiState {
    data object Loading : MoveTenantUiState

    data class Empty(
        val tenantName: String,
    ) : MoveTenantUiState

    data class Content(
        val tenantName: String,
        val targets: List<MoveTargetUiState>,
        val confirmation: MoveConfirmationUiState? = null,
        val isMoving: Boolean = false,
    ) : MoveTenantUiState

    data class Error(
        val message: UiText,
    ) : MoveTenantUiState
}
