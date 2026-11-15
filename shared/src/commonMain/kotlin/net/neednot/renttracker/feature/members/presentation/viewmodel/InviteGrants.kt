package net.neednot.renttracker.feature.members.presentation.viewmodel

import net.neednot.renttracker.core.access.CallerAccess
import net.neednot.renttracker.core.access.PermissionCategory
import net.neednot.renttracker.core.access.PermissionPreset
import net.neednot.renttracker.core.data.AccessLevel
import net.neednot.renttracker.core.data.SettingsAccessLevel

internal fun CallerAccess.limitsInvites(): Boolean =
    !isOwner &&
        (
            PermissionCategory.entries.any { grantableLevels(it).size < AccessLevel.entries.size } ||
                grantableSettingsLevels().size < SettingsAccessLevel.entries.size ||
                !canGrantEditOthers()
        )

internal fun CallerAccess.defaultInvitePreset(): PermissionPreset =
    if (PermissionPreset.Bookkeeper in grantablePresets()) PermissionPreset.Bookkeeper else PermissionPreset.Viewer
