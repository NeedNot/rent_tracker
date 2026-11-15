package net.neednot.renttracker.core.access

import net.neednot.renttracker.core.data.AccessLevel
import net.neednot.renttracker.core.data.MembershipRole
import net.neednot.renttracker.core.data.SettingsAccessLevel
import kotlin.test.Test
import kotlin.test.assertEquals

class PermissionPresetTest {
    @Test
    fun eachPresetMatchesItselfAndNothingElse() {
        PermissionPreset.entries.forEach { preset ->
            assertEquals(preset, PermissionPreset.matching(preset.permissions))
        }
    }

    @Test
    fun aChangedCategoryMatchesNoPreset() {
        val changed = PermissionPreset.Bookkeeper.permissions.copy(tenants = AccessLevel.AddEdit)

        assertEquals(null, PermissionPreset.matching(changed))
    }

    @Test
    fun theEditOthersSwitchIsPartOfThePreset() {
        val managerWithoutSwitch = PermissionPreset.Manager.permissions.copy(editOthers = false)

        assertEquals(null, PermissionPreset.matching(managerWithoutSwitch))
    }

    @Test
    fun theOwnerIsLabelledOwnerWhateverItsStoredLevels() {
        val label = MemberPermissionLabel.of(MembershipRole.Owner, PermissionPreset.Viewer.permissions)

        assertEquals(MemberPermissionLabel.Owner, label)
    }

    @Test
    fun aMemberIsLabelledByTheMatchingPresetOrCustom() {
        val custom = PermissionPreset.Viewer.permissions.copy(settings = SettingsAccessLevel.Full)

        assertEquals(
            listOf(
                MemberPermissionLabel.Viewer,
                MemberPermissionLabel.Bookkeeper,
                MemberPermissionLabel.Manager,
                MemberPermissionLabel.Custom,
                MemberPermissionLabel.Custom,
            ),
            listOf(
                PermissionPreset.Viewer.permissions,
                PermissionPreset.Bookkeeper.permissions,
                PermissionPreset.Manager.permissions,
                PermissionPreset.ownerPermissions,
                custom,
            ).map { MemberPermissionLabel.of(MembershipRole.Member, it) },
        )
    }
}
