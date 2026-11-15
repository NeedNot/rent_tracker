package net.neednot.renttracker.feature.members.domain

import net.neednot.renttracker.core.access.CallerAccess

data class MembersOverview(
    val groupName: String,
    val access: CallerAccess,
    val members: List<Member>,
    val invites: List<PendingInvite>,
) {
    val caller: Member? get() = members.firstOrNull { it.membershipId == access.membershipId }

    fun member(membershipId: String): Member? = members.firstOrNull { it.membershipId == membershipId }
}
