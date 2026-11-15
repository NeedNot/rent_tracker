package net.neednot.renttracker.feature.home.presentation.viewmodel

internal data class HomeOverlay(
    val decliningInviteId: String? = null,
    val workingInviteId: String? = null,
    val isRejectedSheetOpen: Boolean = false,
    val isLateRentPromptShown: Boolean = false,
    val hasAnsweredLateRentPrompt: Boolean = false,
)
