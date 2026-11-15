package net.neednot.renttracker.feature.group.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

sealed interface ArchivedTenantsUiState {
    val offlineBanner: UiText?

    data class Loading(
        override val offlineBanner: UiText? = null,
    ) : ArchivedTenantsUiState

    data class Empty(
        override val offlineBanner: UiText? = null,
    ) : ArchivedTenantsUiState

    data class Content(
        val tenants: List<ArchivedTenantRowUiState>,
        val syncError: UiText? = null,
        override val offlineBanner: UiText? = null,
    ) : ArchivedTenantsUiState

    data class Error(
        val message: UiText,
        override val offlineBanner: UiText? = null,
    ) : ArchivedTenantsUiState
}
