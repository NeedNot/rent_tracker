package net.neednot.renttracker.core.data

import net.neednot.renttracker.model.home.Invitation
import net.neednot.renttracker.model.home.InvitationPermissions
import net.neednot.renttracker.model.members.Invite
import net.neednot.renttracker.model.members.InvitePermissions
import net.neednot.renttracker.model.members.MemberProfile

fun MemberProfile.toEntity(): MemberProfileEntity =
    MemberProfileEntity(accountId = accountId, displayName = displayName, email = email)

fun Invite.toEntity(): InviteEntity =
    InviteEntity(
        id = id,
        groupId = groupId,
        creationTime = creationTime.toLong(),
        email = email,
        permissions = permissions.toPermissionSet(),
        invitedBy = invitedBy,
        expiresAt = expiresAt,
    )

fun Invitation.toEntity(): HomeInvitationEntity =
    HomeInvitationEntity(
        inviteId = inviteId,
        groupId = groupId,
        groupName = groupName,
        invitedByName = invitedByName,
        permissions = permissions.toPermissionSet(),
        expiresAt = expiresAt,
    )

private fun InvitePermissions.toPermissionSet(): PermissionSet =
    PermissionSet(
        tenants = accessLevelOf(tenants),
        rentTerms = accessLevelOf(rentTerms),
        payments = accessLevelOf(payments),
        debts = accessLevelOf(debts),
        members = accessLevelOf(members),
        settings = settingsAccessLevelOf(settings),
        editOthers = editOthers,
    )

private fun InvitationPermissions.toPermissionSet(): PermissionSet =
    PermissionSet(
        tenants = accessLevelOf(tenants),
        rentTerms = accessLevelOf(rentTerms),
        payments = accessLevelOf(payments),
        debts = accessLevelOf(debts),
        members = accessLevelOf(members),
        settings = settingsAccessLevelOf(settings),
        editOthers = editOthers,
    )
