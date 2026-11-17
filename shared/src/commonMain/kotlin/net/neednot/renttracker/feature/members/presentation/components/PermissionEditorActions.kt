package net.neednot.renttracker.feature.members.presentation.components

import net.neednot.renttracker.core.access.PermissionPreset
import net.neednot.renttracker.core.data.AccessLevel
import net.neednot.renttracker.feature.members.presentation.uistate.PermissionArea

internal data class PermissionEditorActions(
    val onPresetClicked: (PermissionPreset) -> Unit = {},
    val onLevelSelected: (PermissionArea, AccessLevel) -> Unit = { _, _ -> },
    val onEditOthersChanged: (Boolean) -> Unit = {},
)
