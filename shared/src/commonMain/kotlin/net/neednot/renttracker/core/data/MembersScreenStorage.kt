package net.neednot.renttracker.core.data

import net.neednot.renttracker.model.members.MembersScreenResult

suspend fun ScreenResultStore.replaceMembers(
    groupId: String,
    result: MembersScreenResult,
) {
    replaceMembers(
        groupId = groupId,
        memberships = result.memberships.map { it.toEntity() },
        memberProfiles = result.memberProfiles.map { it.toEntity() },
        invites = result.invites.map { it.toEntity() },
    )
}
