package net.neednot.renttracker.feature.members.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

data class RemoveMemberDialogUiState(
    val membershipId: String,
    val memberName: String,
    val memberFirstName: String,
    val groupName: String,
    val isWorking: Boolean = false,
    val error: UiText? = null,
)
