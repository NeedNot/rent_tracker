package net.neednot.renttracker.core.access

import net.neednot.renttracker.core.data.AccessLevel
import net.neednot.renttracker.core.data.MembershipRole
import net.neednot.renttracker.core.data.PermissionSet

fun PermissionSet.level(category: PermissionCategory): AccessLevel =
    when (category) {
        PermissionCategory.Tenants -> tenants
        PermissionCategory.RentTerms -> rentTerms
        PermissionCategory.Payments -> payments
        PermissionCategory.Debts -> debts
        PermissionCategory.Members -> members
    }

fun PermissionSet.effectiveFor(role: MembershipRole): PermissionSet =
    if (role == MembershipRole.Owner) PermissionPreset.ownerPermissions else this

val PermissionSet.holdsEditOthers: Boolean
    get() = editOthers || (payments == AccessLevel.Full && debts == AccessLevel.Full)

fun PermissionSet.isAtOrBelow(upper: PermissionSet): Boolean =
    PermissionCategory.entries.all { level(it) <= upper.level(it) } &&
        settings <= upper.settings &&
        (!editOthers || upper.holdsEditOthers)

fun PermissionSet.raisesOnlyWithin(
    current: PermissionSet,
    ceiling: PermissionSet,
): Boolean =
    PermissionCategory.entries.all { level(it) <= current.level(it) || level(it) <= ceiling.level(it) } &&
        (settings <= current.settings || settings <= ceiling.settings) &&
        (!editOthers || current.editOthers || ceiling.holdsEditOthers)
