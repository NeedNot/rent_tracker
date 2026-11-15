package net.neednot.renttracker.feature.members.domain

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import net.neednot.renttracker.core.data.PermissionSet
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced

class FakeMembersRepository : MembersRepository {
    val members = MutableStateFlow<Synced<MembersOverview?, MembersError>>(Synced(null, SyncStatus.Syncing))

    var result: Result<Unit, MembersError> = Ok(Unit)
    var beforeAnswer: (() -> Unit)? = null

    val calls = mutableListOf<MembersRepositoryCall>()

    override fun observeMembers(groupId: String): Flow<Synced<MembersOverview?, MembersError>> = members

    override suspend fun sendInvite(
        groupId: String,
        email: String,
        permissions: PermissionSet,
    ): Result<Unit, MembersError> = answer(MembersRepositoryCall.SendInvite(groupId, email, permissions))

    override suspend fun revokeInvite(inviteId: String): Result<Unit, MembersError> =
        answer(MembersRepositoryCall.RevokeInvite(inviteId))

    override suspend fun updatePermissions(
        membershipId: String,
        expectedVersion: Long,
        permissions: PermissionSet,
    ): Result<Unit, MembersError> =
        answer(MembersRepositoryCall.UpdatePermissions(membershipId, expectedVersion, permissions))

    override suspend fun removeMember(membershipId: String): Result<Unit, MembersError> =
        answer(MembersRepositoryCall.RemoveMember(membershipId))

    override suspend fun transferOwnership(
        groupId: String,
        toMembershipId: String,
    ): Result<Unit, MembersError> = answer(MembersRepositoryCall.TransferOwnership(groupId, toMembershipId))

    private fun answer(call: MembersRepositoryCall): Result<Unit, MembersError> {
        calls += call
        beforeAnswer?.invoke()
        return result
    }
}
