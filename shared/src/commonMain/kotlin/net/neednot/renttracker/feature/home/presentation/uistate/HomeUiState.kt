package net.neednot.renttracker.feature.home.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

sealed interface HomeUiState {
    val accountInitial: String?

    data class Loading(
        override val accountInitial: String? = null,
    ) : HomeUiState

    data class Empty(
        override val accountInitial: String? = null,
    ) : HomeUiState

    data class Content(
        val groups: List<GroupCardUiState>,
        val invitations: List<InvitationCardUiState> = emptyList(),
        val archivedGroupsRow: UiText? = null,
        val declineDialog: DeclineInviteUiState? = null,
        val syncError: UiText? = null,
        override val accountInitial: String? = null,
    ) : HomeUiState

    data class Error(
        val message: UiText,
        override val accountInitial: String? = null,
    ) : HomeUiState
}
