package net.neednot.renttracker.feature.members.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

data class InviteMemberUiState(
    val isLoading: Boolean = true,
    val email: String = "",
    val emailError: UiText? = null,
    val limitedInviterName: String? = null,
    val form: PermissionsFormUiState? = null,
    val isCustomizeExpanded: Boolean = false,
    val error: UiText? = null,
    val isSending: Boolean = false,
)
