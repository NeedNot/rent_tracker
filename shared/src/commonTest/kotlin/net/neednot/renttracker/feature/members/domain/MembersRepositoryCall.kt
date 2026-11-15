package net.neednot.renttracker.feature.members.domain

import net.neednot.renttracker.core.data.PermissionSet

sealed interface MembersRepositoryCall {
    data class SendInvite(
        val groupId: String,
        val email: String,
        val permissions: PermissionSet,
    ) : MembersRepositoryCall

    data class RevokeInvite(
        val inviteId: String,
    ) : MembersRepositoryCall

    data class UpdatePermissions(
        val membershipId: String,
        val expectedVersion: Long,
        val permissions: PermissionSet,
    ) : MembersRepositoryCall

    data class RemoveMember(
        val membershipId: String,
    ) : MembersRepositoryCall

    data class TransferOwnership(
        val groupId: String,
        val toMembershipId: String,
    ) : MembersRepositoryCall
}
