package net.neednot.renttracker.feature.group.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

sealed interface GroupUiState {
    val offlineBanner: UiText?

    data class Loading(
        override val offlineBanner: UiText? = null,
    ) : GroupUiState

    data class Empty(
        val groupName: String,
        val archived: UiText?,
        val chrome: GroupChromeUiState,
        override val offlineBanner: UiText? = null,
    ) : GroupUiState

    data class Content(
        val groupName: String,
        val balance: UiText,
        val tenantCounts: UiText,
        val archived: UiText?,
        val tenants: List<TenantRowUiState>,
        val chrome: GroupChromeUiState,
        val syncError: UiText? = null,
        override val offlineBanner: UiText? = null,
    ) : GroupUiState

    data class Error(
        val message: UiText,
        override val offlineBanner: UiText? = null,
    ) : GroupUiState
}
