package net.neednot.renttracker.core.access

import net.neednot.renttracker.core.data.MembershipRole
import net.neednot.renttracker.core.data.PermissionSet

enum class MemberPermissionLabel {
    Owner,
    Viewer,
    Bookkeeper,
    Manager,
    Custom,
    ;

    companion object {
        fun of(
            role: MembershipRole,
            permissions: PermissionSet,
        ): MemberPermissionLabel {
            if (role == MembershipRole.Owner) return Owner
            return when (PermissionPreset.matching(permissions)) {
                PermissionPreset.Viewer -> Viewer
                PermissionPreset.Bookkeeper -> Bookkeeper
                PermissionPreset.Manager -> Manager
                null -> Custom
            }
        }
    }
}
