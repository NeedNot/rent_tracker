package net.neednot.renttracker.feature.group.presentation.uistate

data class GroupChromeUiState(
    val isViewOnly: Boolean = false,
    val isArchived: Boolean = false,
    val canAddTenants: Boolean = false,
    val canUnarchive: Boolean = true,
    val menuItems: List<GroupMenuItem> = emptyList(),
    val dialog: GroupDialogUiState? = null,
    val isWorking: Boolean = false,
)
