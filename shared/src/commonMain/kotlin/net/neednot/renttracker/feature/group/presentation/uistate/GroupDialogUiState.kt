package net.neednot.renttracker.feature.group.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

sealed interface GroupDialogUiState {
    val groupName: String

    data class Archive(
        override val groupName: String,
        val tenantCount: UiText,
        val balance: UiText,
    ) : GroupDialogUiState

    data class Leave(
        override val groupName: String,
    ) : GroupDialogUiState

    data class LeaveOwner(
        override val groupName: String,
    ) : GroupDialogUiState
}
