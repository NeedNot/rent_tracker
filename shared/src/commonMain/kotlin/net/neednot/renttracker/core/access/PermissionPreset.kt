package net.neednot.renttracker.core.access

import net.neednot.renttracker.core.data.AccessLevel
import net.neednot.renttracker.core.data.PermissionSet
import net.neednot.renttracker.core.data.SettingsAccessLevel

enum class PermissionPreset(
    val permissions: PermissionSet,
) {
    Viewer(
        PermissionSet(
            tenants = AccessLevel.Read,
            rentTerms = AccessLevel.Read,
            payments = AccessLevel.Read,
            debts = AccessLevel.Read,
            members = AccessLevel.Read,
            settings = SettingsAccessLevel.Read,
            editOthers = false,
        ),
    ),
    Bookkeeper(
        PermissionSet(
            tenants = AccessLevel.Read,
            rentTerms = AccessLevel.Read,
            payments = AccessLevel.AddEdit,
            debts = AccessLevel.AddEdit,
            members = AccessLevel.Read,
            settings = SettingsAccessLevel.Read,
            editOthers = false,
        ),
    ),
    Manager(
        PermissionSet(
            tenants = AccessLevel.AddEdit,
            rentTerms = AccessLevel.AddEdit,
            payments = AccessLevel.AddEdit,
            debts = AccessLevel.AddEdit,
            members = AccessLevel.Read,
            settings = SettingsAccessLevel.Read,
            editOthers = true,
        ),
    ),
    ;

    companion object {
        val ownerPermissions =
            PermissionSet(
                tenants = AccessLevel.Full,
                rentTerms = AccessLevel.Full,
                payments = AccessLevel.Full,
                debts = AccessLevel.Full,
                members = AccessLevel.Full,
                settings = SettingsAccessLevel.Full,
                editOthers = true,
            )

        fun matching(permissions: PermissionSet): PermissionPreset? =
            entries.firstOrNull {
                it.permissions ==
                    permissions
            }
    }
}
