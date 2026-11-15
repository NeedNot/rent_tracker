package net.neednot.renttracker.feature.members.data

import net.neednot.renttracker.core.data.PermissionSet
import net.neednot.renttracker.core.data.toConvex
import net.neednot.renttracker.model.invites.InvitesSendPermissions
import net.neednot.renttracker.model.members.MembersUpdatePermissionsPermissions

internal fun PermissionSet.toInvitePermissions(): InvitesSendPermissions =
    InvitesSendPermissions(
        tenants = tenants.toConvex(),
        rentTerms = rentTerms.toConvex(),
        payments = payments.toConvex(),
        debts = debts.toConvex(),
        members = members.toConvex(),
        settings = settings.toConvex(),
        editOthers = editOthers,
    )

internal fun PermissionSet.toUpdatedPermissions(): MembersUpdatePermissionsPermissions =
    MembersUpdatePermissionsPermissions(
        tenants = tenants.toConvex(),
        rentTerms = rentTerms.toConvex(),
        payments = payments.toConvex(),
        debts = debts.toConvex(),
        members = members.toConvex(),
        settings = settings.toConvex(),
        editOthers = editOthers,
    )
