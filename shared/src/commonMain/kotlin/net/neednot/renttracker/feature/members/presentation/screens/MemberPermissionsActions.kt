package net.neednot.renttracker.feature.members.presentation.screens

import net.neednot.renttracker.feature.members.presentation.components.PermissionEditorActions

internal data class MemberPermissionsActions(
    val editor: PermissionEditorActions = PermissionEditorActions(),
    val onSaveClicked: () -> Unit = {},
    val onRemoveClicked: () -> Unit = {},
    val onRemoveConfirmed: () -> Unit = {},
    val onRemoveDismissed: () -> Unit = {},
    val onCloseClicked: () -> Unit = {},
)
