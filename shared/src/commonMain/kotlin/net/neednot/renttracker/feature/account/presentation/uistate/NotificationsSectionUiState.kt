package net.neednot.renttracker.feature.account.presentation.uistate

sealed interface NotificationsSectionUiState {
    data object Hidden : NotificationsSectionUiState

    data class Shown(
        val groups: List<GroupNotificationsRowUiState>,
        val isBlockedOnPhone: Boolean,
    ) : NotificationsSectionUiState
}
