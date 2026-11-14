package net.neednot.renttracker.feature.group.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

sealed interface ArchivedTenantsUiState {
    data object Loading : ArchivedTenantsUiState

    data object Empty : ArchivedTenantsUiState

    data class Content(
        val tenants: List<ArchivedTenantRowUiState>,
        val syncError: UiText? = null,
    ) : ArchivedTenantsUiState

    data class Error(
        val message: UiText,
    ) : ArchivedTenantsUiState
}
