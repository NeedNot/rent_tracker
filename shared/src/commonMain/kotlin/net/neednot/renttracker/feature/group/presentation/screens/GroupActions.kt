package net.neednot.renttracker.feature.group.presentation.screens

import net.neednot.renttracker.feature.group.presentation.uistate.GroupDialogUiState
import net.neednot.renttracker.feature.group.presentation.uistate.GroupMenuItem

internal data class GroupActions(
    val onBackClicked: () -> Unit = {},
    val onTenantClicked: (tenantId: String) -> Unit = {},
    val onAddTenantClicked: () -> Unit = {},
    val onArchivedClicked: () -> Unit = {},
    val onMenuItemClicked: (GroupMenuItem) -> Unit = {},
    val onUnarchiveClicked: () -> Unit = {},
    val onDialogConfirmed: (GroupDialogUiState) -> Unit = {},
    val onDialogDismissed: () -> Unit = {},
)
