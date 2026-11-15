package net.neednot.renttracker.feature.home.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

data class InvitationCardUiState(
    val inviteId: String,
    val groupName: String,
    val summary: UiText,
    val description: UiText,
    val isWorking: Boolean,
)
