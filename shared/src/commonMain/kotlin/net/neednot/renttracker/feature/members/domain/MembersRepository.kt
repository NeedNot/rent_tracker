package net.neednot.renttracker.feature.members.domain

import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.Flow
import net.neednot.renttracker.core.data.PermissionSet
import net.neednot.renttracker.core.sync.Synced

interface MembersRepository {
    fun observeMembers(groupId: String): Flow<Synced<MembersOverview?, MembersError>>

    suspend fun sendInvite(
        groupId: String,
        email: String,
        permissions: PermissionSet,
    ): Result<Unit, MembersError>

    suspend fun revokeInvite(inviteId: String): Result<Unit, MembersError>

    suspend fun updatePermissions(
        membershipId: String,
        expectedVersion: Long,
        permissions: PermissionSet,
    ): Result<Unit, MembersError>

    suspend fun removeMember(membershipId: String): Result<Unit, MembersError>

    suspend fun transferOwnership(
        groupId: String,
        toMembershipId: String,
    ): Result<Unit, MembersError>
}
