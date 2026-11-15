package net.neednot.renttracker.feature.group.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

sealed interface GroupUiState {
    data object Loading : GroupUiState

    data class Empty(
        val groupName: String,
        val archived: UiText?,
        val chrome: GroupChromeUiState,
    ) : GroupUiState

    data class Content(
        val groupName: String,
        val balance: UiText,
        val tenantCounts: UiText,
        val archived: UiText?,
        val tenants: List<TenantRowUiState>,
        val chrome: GroupChromeUiState,
        val syncError: UiText? = null,
    ) : GroupUiState

    data class Error(
        val message: UiText,
    ) : GroupUiState
}
