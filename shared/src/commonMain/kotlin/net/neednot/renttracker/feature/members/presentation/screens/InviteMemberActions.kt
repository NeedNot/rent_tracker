package net.neednot.renttracker.feature.members.presentation.screens

import net.neednot.renttracker.feature.members.presentation.components.PermissionEditorActions

internal data class InviteMemberActions(
    val editor: PermissionEditorActions = PermissionEditorActions(),
    val onEmailChanged: (String) -> Unit = {},
    val onCustomizeClicked: () -> Unit = {},
    val onSendClicked: () -> Unit = {},
    val onCloseClicked: () -> Unit = {},
)
