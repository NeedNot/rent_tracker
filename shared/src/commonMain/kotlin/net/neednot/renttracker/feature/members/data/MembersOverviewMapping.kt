package net.neednot.renttracker.feature.members.data

import net.neednot.renttracker.core.access.CallerAccess
import net.neednot.renttracker.core.data.GroupEntity
import net.neednot.renttracker.core.data.InviteEntity
import net.neednot.renttracker.core.data.MemberProfileEntity
import net.neednot.renttracker.core.data.MembershipEntity
import net.neednot.renttracker.feature.members.domain.Member
import net.neednot.renttracker.feature.members.domain.MembersOverview
import net.neednot.renttracker.feature.members.domain.PendingInvite
import kotlin.time.Instant

internal fun GroupEntity.toMembersOverview(
    access: CallerAccess,
    memberships: List<MembershipEntity>,
    profiles: List<MemberProfileEntity>,
    invites: List<InviteEntity>,
): MembersOverview {
    val profilesByAccount = profiles.associateBy { it.accountId }
    return MembersOverview(
        groupName = name,
        access = access,
        members =
            memberships.mapNotNull { membership ->
                profilesByAccount[membership.accountId]?.let(membership::toMember)
            },
        invites = invites.map { it.toPendingInvite() },
    )
}

private fun MembershipEntity.toMember(profile: MemberProfileEntity): Member =
    Member(
        membershipId = id,
        accountId = accountId,
        displayName = profile.displayName,
        email = profile.email,
        role = role,
        permissions = permissions,
        version = version,
    )

private fun InviteEntity.toPendingInvite(): PendingInvite =
    PendingInvite(
        inviteId = id,
        email = email,
        permissions = permissions,
        expiresAt = Instant.fromEpochMilliseconds(expiresAt),
    )
