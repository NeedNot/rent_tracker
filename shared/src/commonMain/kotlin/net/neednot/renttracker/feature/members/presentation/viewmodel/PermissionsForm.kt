package net.neednot.renttracker.feature.members.presentation.viewmodel

import net.neednot.renttracker.core.access.CallerAccess
import net.neednot.renttracker.core.access.level
import net.neednot.renttracker.core.data.AccessLevel
import net.neednot.renttracker.core.data.PermissionSet
import net.neednot.renttracker.core.data.SettingsAccessLevel
import net.neednot.renttracker.feature.members.presentation.uistate.EditOthersUiState
import net.neednot.renttracker.feature.members.presentation.uistate.LevelRowUiState
import net.neednot.renttracker.feature.members.presentation.uistate.PermissionArea
import net.neednot.renttracker.feature.members.presentation.uistate.PermissionsFormUiState

internal fun CallerAccess.permissionsForm(
    permissions: PermissionSet,
    saved: PermissionSet? = null,
): PermissionsFormUiState =
    PermissionsFormUiState(
        permissions = permissions,
        presets = grantablePresets(saved),
        levelRows = PermissionArea.entries.map { levelRow(it, permissions, saved) },
        editOthers =
            EditOthersUiState(
                isOn = permissions.editOthers,
                appliesNow = permissions.editOthersApplies,
                isEnabled = permissions.editOthersApplies && canGrantEditOthers(saved?.editOthers ?: false),
            ),
    )

internal fun PermissionSet.withLevel(
    area: PermissionArea,
    level: AccessLevel,
): PermissionSet {
    val changed =
        when (area) {
            PermissionArea.Tenants -> copy(tenants = level)
            PermissionArea.RentTerms -> copy(rentTerms = level)
            PermissionArea.Payments -> copy(payments = level)
            PermissionArea.Debts -> copy(debts = level)
            PermissionArea.Members -> copy(members = level)
            PermissionArea.Settings -> copy(settings = SettingsAccessLevel.valueOf(level.name))
        }
    return if (changed.editOthersApplies) changed else changed.copy(editOthers = false)
}

private val PermissionSet.editOthersApplies: Boolean
    get() = payments == AccessLevel.AddEdit || debts == AccessLevel.AddEdit

private fun CallerAccess.levelRow(
    area: PermissionArea,
    permissions: PermissionSet,
    saved: PermissionSet?,
): LevelRowUiState {
    val category = area.category
    return if (category == null) {
        LevelRowUiState(
            area = area,
            levels = grantableSettingsLevels(saved?.settings).map { it.asAccessLevel() },
            selected = permissions.settings.asAccessLevel(),
        )
    } else {
        LevelRowUiState(
            area = area,
            levels = grantableLevels(category, saved?.level(category)),
            selected = permissions.level(category),
        )
    }
}

private fun SettingsAccessLevel.asAccessLevel(): AccessLevel = AccessLevel.valueOf(name)
