package net.neednot.renttracker.feature.members.domain

import net.neednot.renttracker.core.access.CallerAccess
import net.neednot.renttracker.core.access.PermissionPreset
import net.neednot.renttracker.core.data.AccessLevel
import net.neednot.renttracker.core.data.MembershipRole
import net.neednot.renttracker.core.data.PermissionSet
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced
import kotlin.time.Instant

val kevinsPermissions: PermissionSet =
    PermissionPreset.Bookkeeper.permissions.copy(members = AccessLevel.AddEdit)

val needNot = member("m1", "owner", "Need_Not", MembershipRole.Owner, PermissionPreset.ownerPermissions)
val sam = member("m2", "sam", "Sam Rivera", permissions = PermissionPreset.Manager.permissions)
val kevin = member("m3", "kevin", "Kevin Cho", permissions = kevinsPermissions)
val maria = member("m4", "maria", "Maria Lopez", permissions = PermissionPreset.Bookkeeper.permissions)
val jen = member("m5", "jen", "Jen Park", permissions = PermissionPreset.Viewer.permissions)

val alexInvite =
    PendingInvite(
        inviteId = "i1",
        email = "alex.m@example.com",
        permissions = PermissionPreset.Bookkeeper.permissions,
        expiresAt = Instant.parse("2026-03-16T18:00:00Z"),
    )

fun member(
    membershipId: String,
    accountId: String,
    displayName: String,
    role: MembershipRole = MembershipRole.Member,
    permissions: PermissionSet,
    version: Long = 3,
) = Member(
    membershipId = membershipId,
    accountId = accountId,
    displayName = displayName,
    email = "$accountId@example.com",
    role = role,
    permissions = permissions,
    version = version,
)

fun accessOf(
    caller: Member,
    groupArchived: Boolean = false,
) = CallerAccess(
    membershipId = caller.membershipId,
    accountId = caller.accountId,
    role = caller.role,
    permissions = if (caller.role == MembershipRole.Owner) PermissionPreset.ownerPermissions else caller.permissions,
    groupArchived = groupArchived,
)

fun overviewSeenBy(
    caller: Member,
    members: List<Member> = listOf(needNot, sam, kevin, maria, jen),
    invites: List<PendingInvite> = listOf(alexInvite),
) = MembersOverview(
    groupName = "1st Street",
    access = accessOf(caller),
    members = members,
    invites = invites,
)

fun synced(overview: MembersOverview?): Synced<MembersOverview?, MembersError> = Synced(overview, SyncStatus.Synced)
