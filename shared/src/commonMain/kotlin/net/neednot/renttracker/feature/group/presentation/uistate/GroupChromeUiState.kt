package net.neednot.renttracker.feature.group.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

data class GroupChromeUiState(
    val isViewOnly: Boolean = false,
    val canAddTenants: Boolean = false,
    val canUnarchive: Boolean = true,
    val menuItems: List<GroupMenuItem> = emptyList(),
    val archivedBanner: UiText? = null,
    val dialog: GroupDialogUiState? = null,
    val isWorking: Boolean = false,
)
