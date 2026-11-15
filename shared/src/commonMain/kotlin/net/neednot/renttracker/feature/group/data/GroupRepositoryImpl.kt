package net.neednot.renttracker.feature.group.data

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getError
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import net.neednot.renttracker.core.access.observeCallerAccess
import net.neednot.renttracker.core.backend.Backend
import net.neednot.renttracker.core.backend.ConvexJson
import net.neednot.renttracker.core.data.AsOfClock
import net.neednot.renttracker.core.data.GroupAccessWatcher
import net.neednot.renttracker.core.data.GroupScopeEntities
import net.neednot.renttracker.core.data.RentTrackerDatabase
import net.neednot.renttracker.core.data.ScreenResultStore
import net.neednot.renttracker.core.data.TenantStatus
import net.neednot.renttracker.core.data.toEntity
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.sync.syncedFlow
import net.neednot.renttracker.core.telemetry.Severity
import net.neednot.renttracker.core.telemetry.Telemetry
import net.neednot.renttracker.feature.group.domain.GroupDetails
import net.neednot.renttracker.feature.group.domain.GroupError
import net.neednot.renttracker.feature.group.domain.GroupRepository
import net.neednot.renttracker.feature.group.domain.GroupSettings
import net.neednot.renttracker.feature.group.domain.GroupTenant
import net.neednot.renttracker.model.groups.GroupsArchiveArgs
import net.neednot.renttracker.model.groups.GroupsScreenArgs
import net.neednot.renttracker.model.groups.GroupsScreenResult
import net.neednot.renttracker.model.groups.GroupsUnarchiveArgs
import net.neednot.renttracker.model.groups.GroupsUpdateSettingsArgs
import net.neednot.renttracker.model.members.MembersLeaveArgs

class GroupRepositoryImpl(
    private val backend: Backend,
    private val asOfClock: AsOfClock,
    private val database: RentTrackerDatabase,
    private val screenResultStore: ScreenResultStore,
    private val groupAccessWatcher: GroupAccessWatcher,
    private val telemetry: Telemetry,
) : GroupRepository {
    private val groupDao = database.groupDao()
    private val groupSummaryDao = database.groupSummaryDao()
    private val tenantDao = database.tenantDao()
    private val tenantSummaryDao = database.tenantSummaryDao()
    private val calls = GroupBackendCalls(backend, telemetry)
    private val leavingGroupIds = MutableStateFlow(emptySet<String>())

    override fun observeGroup(groupId: String): Flow<Synced<GroupDetails?, GroupError>> =
        syncedFlow(
            cached = cachedGroup(groupId),
            serverResults = groupScreenResults(groupId),
            store = { result -> store(groupId, result) },
        )

    private fun cachedGroup(groupId: String): Flow<GroupDetails?> =
        combine(
            groupDao.observeById(groupId),
            groupSummaryDao.observeByGroupId(groupId),
            cachedTenants(groupId, TenantStatus.Active),
            cachedTenants(groupId, TenantStatus.Archived),
            database.observeCallerAccess(groupId),
        ) { group, summary, activeTenants, archivedTenants, access ->
            if (group == null || summary == null || access == null) return@combine null
            group.toGroupDetails(summary, activeTenants, archivedTenants, access)
        }

    private fun cachedTenants(
        groupId: String,
        status: TenantStatus,
    ): Flow<List<GroupTenant>> =
        combine(
            tenantDao.observeByGroupAndStatus(groupId, status),
            tenantSummaryDao.observeByGroupAndStatus(groupId, status),
        ) { tenants, summaries -> tenants.toGroupTenants(summaries) }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun groupScreenResults(groupId: String): Flow<Result<GroupsScreenResult, GroupError>> =
        asOfClock.asOf
            .flatMapLatest { asOf ->
                groupAccessWatcher.watchGroup(
                    groupId,
                    backend
                        .subscribe(
                            name = GROUP_SCREEN,
                            args =
                                ConvexJson.encodeArgs(
                                    GroupsScreenArgs.serializer(),
                                    GroupsScreenArgs(asOf = asOf, groupId = groupId),
                                ),
                            resultSerializer = GroupsScreenResult.serializer(),
                        ).filterNot { result ->
                            groupId in leavingGroupIds.value &&
                                result.getError()?.isPermissionDenied() == true
                        },
                )
            }.map { result -> result.mapError { it.toGroupError() }.reportingUnexpected() }

    override suspend fun updateSettings(
        groupId: String,
        expectedVersion: Long,
        settings: GroupSettings,
    ): Result<Unit, GroupError> =
        calls.mutate(
            UPDATE_SETTINGS,
            settings.toUpdateArgs(groupId, expectedVersion),
            GroupsUpdateSettingsArgs.serializer(),
        )

    override suspend fun archiveGroup(groupId: String): Result<Unit, GroupError> =
        calls.mutate(ARCHIVE_GROUP, GroupsArchiveArgs(groupId), GroupsArchiveArgs.serializer())

    override suspend fun unarchiveGroup(groupId: String): Result<Unit, GroupError> =
        calls.mutate(UNARCHIVE_GROUP, GroupsUnarchiveArgs(groupId), GroupsUnarchiveArgs.serializer())

    override suspend fun leaveGroup(groupId: String): Result<Unit, GroupError> {
        leavingGroupIds.update { it + groupId }
        return calls
            .mutate(LEAVE_GROUP, MembersLeaveArgs(groupId), MembersLeaveArgs.serializer())
            .onOk { screenResultStore.clearGroup(groupId) }
            .onErr { leavingGroupIds.update { it - groupId } }
    }

    private suspend fun store(
        groupId: String,
        result: GroupsScreenResult,
    ) {
        screenResultStore.replaceGroup(
            groupId = groupId,
            scope =
                GroupScopeEntities(
                    groups = result.groups.map { it.toEntity() },
                    tenants = result.tenants.map { it.toEntity() },
                    tenantSummaries = result.tenantSummaries.map { it.toEntity() },
                    groupSummaries = result.groupSummaries.map { it.toEntity() },
                    callerMemberships = result.memberships.map { it.toEntity() },
                ),
        )
    }

    private fun <T> Result<T, GroupError>.reportingUnexpected(): Result<T, GroupError> =
        onErr { error ->
            if (error is GroupError.Unexpected) telemetry.log(TAG, error.message, Severity.Error)
        }

    private companion object {
        const val TAG = "GroupRepository"
        const val GROUP_SCREEN = "groups:screen"
        const val UPDATE_SETTINGS = "groups:updateSettings"
        const val ARCHIVE_GROUP = "groups:archive"
        const val UNARCHIVE_GROUP = "groups:unarchive"
        const val LEAVE_GROUP = "members:leave"
    }
}
