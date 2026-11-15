package net.neednot.renttracker.feature.home.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

data class DeclineInviteUiState(
    val inviteId: String,
    val title: UiText,
    val message: UiText,
)
