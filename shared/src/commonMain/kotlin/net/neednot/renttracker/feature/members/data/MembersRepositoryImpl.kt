package net.neednot.renttracker.feature.members.data

import com.github.michaelbull.result.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import net.neednot.renttracker.core.access.observeCallerAccess
import net.neednot.renttracker.core.backend.Backend
import net.neednot.renttracker.core.backend.ConvexJson
import net.neednot.renttracker.core.data.AsOfClock
import net.neednot.renttracker.core.data.GroupAccessWatcher
import net.neednot.renttracker.core.data.PermissionSet
import net.neednot.renttracker.core.data.RentTrackerDatabase
import net.neednot.renttracker.core.data.ScreenResultStore
import net.neednot.renttracker.core.data.replaceMembers
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.sync.syncedFlow
import net.neednot.renttracker.core.telemetry.Telemetry
import net.neednot.renttracker.feature.members.domain.MembersError
import net.neednot.renttracker.feature.members.domain.MembersOverview
import net.neednot.renttracker.feature.members.domain.MembersRepository
import net.neednot.renttracker.model.invites.InvitesRevokeArgs
import net.neednot.renttracker.model.invites.InvitesSendArgs
import net.neednot.renttracker.model.members.MembersRemoveArgs
import net.neednot.renttracker.model.members.MembersScreenArgs
import net.neednot.renttracker.model.members.MembersScreenResult
import net.neednot.renttracker.model.members.MembersTransferOwnershipArgs
import net.neednot.renttracker.model.members.MembersUpdatePermissionsArgs

class MembersRepositoryImpl(
    private val backend: Backend,
    private val asOfClock: AsOfClock,
    private val database: RentTrackerDatabase,
    private val screenResultStore: ScreenResultStore,
    private val groupAccessWatcher: GroupAccessWatcher,
    telemetry: Telemetry,
) : MembersRepository {
    private val calls = MembersBackendCalls(backend, telemetry)

    override fun observeMembers(groupId: String): Flow<Synced<MembersOverview?, MembersError>> =
        syncedFlow(
            cached = cachedMembers(groupId),
            serverResults = membersScreenResults(groupId),
            store = { result -> screenResultStore.replaceMembers(groupId, result) },
        )

    override suspend fun sendInvite(
        groupId: String,
        email: String,
        permissions: PermissionSet,
    ): Result<Unit, MembersError> =
        calls.mutate(
            name = SEND_INVITE,
            args = InvitesSendArgs(email = email, groupId = groupId, permissions = permissions.toInvitePermissions()),
            argsSerializer = InvitesSendArgs.serializer(),
        )

    override suspend fun revokeInvite(inviteId: String): Result<Unit, MembersError> =
        calls.mutate(
            name = REVOKE_INVITE,
            args = InvitesRevokeArgs(inviteId = inviteId),
            argsSerializer = InvitesRevokeArgs.serializer(),
        )

    override suspend fun updatePermissions(
        membershipId: String,
        expectedVersion: Long,
        permissions: PermissionSet,
    ): Result<Unit, MembersError> =
        calls.mutate(
            name = UPDATE_PERMISSIONS,
            args =
                MembersUpdatePermissionsArgs(
                    expectedVersion = expectedVersion,
                    membershipId = membershipId,
                    permissions = permissions.toUpdatedPermissions(),
                ),
            argsSerializer = MembersUpdatePermissionsArgs.serializer(),
        )

    override suspend fun removeMember(membershipId: String): Result<Unit, MembersError> =
        calls.mutate(
            name = REMOVE_MEMBER,
            args = MembersRemoveArgs(membershipId = membershipId),
            argsSerializer = MembersRemoveArgs.serializer(),
        )

    override suspend fun transferOwnership(
        groupId: String,
        toMembershipId: String,
    ): Result<Unit, MembersError> =
        calls.mutate(
            name = TRANSFER_OWNERSHIP,
            args = MembersTransferOwnershipArgs(groupId = groupId, toMembershipId = toMembershipId),
            argsSerializer = MembersTransferOwnershipArgs.serializer(),
        )

    private fun cachedMembers(groupId: String): Flow<MembersOverview?> =
        combine(
            database.groupDao().observeById(groupId),
            database.observeCallerAccess(groupId),
            database.membershipDao().observeByGroupId(groupId),
            database.memberProfileDao().observeByGroupId(groupId),
            database.inviteDao().observeByGroupId(groupId),
        ) { group, access, memberships, profiles, invites ->
            if (group == null || access == null) return@combine null
            group.toMembersOverview(access, memberships, profiles, invites)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun membersScreenResults(groupId: String): Flow<Result<MembersScreenResult, MembersError>> =
        asOfClock.asOf
            .flatMapLatest { asOf ->
                groupAccessWatcher.watchGroup(
                    groupId,
                    backend.subscribe(
                        name = MEMBERS_SCREEN,
                        args =
                            ConvexJson.encodeArgs(
                                MembersScreenArgs.serializer(),
                                MembersScreenArgs(asOf = asOf, groupId = groupId),
                            ),
                        resultSerializer = MembersScreenResult.serializer(),
                    ),
                )
            }.map { result -> calls.membersResultOf(result) }

    private companion object {
        const val MEMBERS_SCREEN = "members:screen"
        const val SEND_INVITE = "invites:send"
        const val REVOKE_INVITE = "invites:revoke"
        const val UPDATE_PERMISSIONS = "members:updatePermissions"
        const val REMOVE_MEMBER = "members:remove"
        const val TRANSFER_OWNERSHIP = "members:transferOwnership"
    }
}
