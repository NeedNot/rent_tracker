package net.neednot.renttracker.feature.members.presentation.uistate

import net.neednot.renttracker.core.access.PermissionPreset
import net.neednot.renttracker.core.data.PermissionSet

data class PermissionsFormUiState(
    val permissions: PermissionSet,
    val presets: List<PermissionPreset>,
    val levelRows: List<LevelRowUiState>,
    val editOthers: EditOthersUiState,
) {
    val selectedPreset: PermissionPreset? get() = PermissionPreset.matching(permissions)
}
