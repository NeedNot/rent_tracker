package net.neednot.renttracker.core.access

import net.neednot.renttracker.core.data.AccessLevel
import net.neednot.renttracker.core.data.MembershipRole
import net.neednot.renttracker.core.data.PermissionSet
import net.neednot.renttracker.core.data.SettingsAccessLevel

data class CallerAccess(
    val membershipId: String,
    val accountId: String,
    val role: MembershipRole,
    val permissions: PermissionSet,
    val groupArchived: Boolean,
) {
    val isOwner: Boolean get() = role == MembershipRole.Owner

    val isViewOnly: Boolean
        get() =
            !isOwner &&
                PermissionCategory.entries.all { permissions.level(it) == AccessLevel.Read } &&
                permissions.settings == SettingsAccessLevel.Read

    val canManageMembers: Boolean get() = isOpen && permissions.members >= AccessLevel.AddEdit

    val canEditPermissions: Boolean get() = isOpen && permissions.members == AccessLevel.Full

    val canEditSettings: Boolean get() = isOpen && permissions.settings == SettingsAccessLevel.Full

    val canMoveTenants: Boolean get() = isOpen && permissions.tenants == AccessLevel.Full

    private val isOpen: Boolean get() = !groupArchived

    fun isYou(accountId: String): Boolean = accountId == this.accountId

    fun grantableLevels(
        category: PermissionCategory,
        currentLevel: AccessLevel? = null,
    ): List<AccessLevel> =
        AccessLevel.entries.filter { level ->
            level <= permissions.level(category) || (currentLevel != null && level <= currentLevel)
        }

    fun grantableSettingsLevels(currentLevel: SettingsAccessLevel? = null): List<SettingsAccessLevel> =
        SettingsAccessLevel.entries.filter { level ->
            level <= permissions.settings || (currentLevel != null && level <= currentLevel)
        }

    fun canGrantEditOthers(currentlyOn: Boolean = false): Boolean = currentlyOn || permissions.holdsEditOthers

    fun mayGrant(
        proposed: PermissionSet,
        current: PermissionSet? = null,
    ): Boolean =
        if (current == null) {
            proposed.isAtOrBelow(permissions)
        } else {
            proposed.raisesOnlyWithin(current, permissions)
        }

    fun grantablePresets(current: PermissionSet? = null): List<PermissionPreset> =
        PermissionPreset.entries.filter { mayGrant(it.permissions, current) }

    fun canRemove(
        role: MembershipRole,
        permissions: PermissionSet,
        accountId: String,
    ): Boolean =
        canManageMembers &&
            role != MembershipRole.Owner &&
            !isYou(accountId) &&
            (this.permissions.members == AccessLevel.Full || permissions.isAtOrBelow(this.permissions))
}
