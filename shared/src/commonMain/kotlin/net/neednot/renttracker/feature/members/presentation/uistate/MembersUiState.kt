package net.neednot.renttracker.feature.members.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

sealed interface MembersUiState {
    data object Loading : MembersUiState

    data class Content(
        val groupName: String,
        val showsViewOnlyInfo: Boolean,
        val members: List<MemberRowUiState>,
        val invites: List<InviteRowUiState>,
        val canInvite: Boolean,
        val canTransferOwnership: Boolean,
        val syncError: UiText? = null,
        val removeDialog: RemoveMemberDialogUiState? = null,
        val offlineBanner: UiText? = null,
    ) : MembersUiState

    data class Error(
        val message: UiText,
    ) : MembersUiState
}
