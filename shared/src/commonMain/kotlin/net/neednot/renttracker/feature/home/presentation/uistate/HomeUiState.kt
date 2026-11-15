package net.neednot.renttracker.feature.home.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

sealed interface HomeUiState {
    val accountInitial: String?
    val offlineBanner: UiText?
    val rejectedPayments: RejectedPaymentsUiState?
    val canCreateGroup: Boolean get() = offlineBanner == null

    data class Loading(
        override val accountInitial: String? = null,
        override val offlineBanner: UiText? = null,
        override val rejectedPayments: RejectedPaymentsUiState? = null,
    ) : HomeUiState

    data class Empty(
        override val accountInitial: String? = null,
        override val offlineBanner: UiText? = null,
        override val rejectedPayments: RejectedPaymentsUiState? = null,
    ) : HomeUiState

    data class Content(
        val groups: List<GroupCardUiState>,
        val invitations: List<InvitationCardUiState> = emptyList(),
        val archivedGroupsRow: UiText? = null,
        val declineDialog: DeclineInviteUiState? = null,
        val syncError: UiText? = null,
        override val accountInitial: String? = null,
        override val offlineBanner: UiText? = null,
        override val rejectedPayments: RejectedPaymentsUiState? = null,
    ) : HomeUiState

    data class Error(
        val message: UiText,
        override val accountInitial: String? = null,
        override val offlineBanner: UiText? = null,
        override val rejectedPayments: RejectedPaymentsUiState? = null,
    ) : HomeUiState
}
