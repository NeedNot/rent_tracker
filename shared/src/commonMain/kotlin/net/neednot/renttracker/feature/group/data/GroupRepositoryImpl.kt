package net.neednot.renttracker.feature.group.data

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onErr
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import net.neednot.renttracker.core.backend.Backend
import net.neednot.renttracker.core.backend.ConvexJson
import net.neednot.renttracker.core.data.AsOfClock
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
import net.neednot.renttracker.feature.group.domain.GroupTenant
import net.neednot.renttracker.model.groups.GroupsScreenArgs
import net.neednot.renttracker.model.groups.GroupsScreenResult

class GroupRepositoryImpl(
    private val backend: Backend,
    private val asOfClock: AsOfClock,
    database: RentTrackerDatabase,
    private val screenResultStore: ScreenResultStore,
    private val telemetry: Telemetry,
) : GroupRepository {
    private val groupDao = database.groupDao()
    private val groupSummaryDao = database.groupSummaryDao()
    private val tenantDao = database.tenantDao()
    private val tenantSummaryDao = database.tenantSummaryDao()

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
        ) { group, summary, activeTenants, archivedTenants ->
            if (group == null || summary == null) return@combine null
            group.toGroupDetails(summary, activeTenants, archivedTenants)
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
                backend.subscribe(
                    name = GROUP_SCREEN,
                    args =
                        ConvexJson.encodeArgs(
                            GroupsScreenArgs.serializer(),
                            GroupsScreenArgs(asOf = asOf, groupId = groupId),
                        ),
                    resultSerializer = GroupsScreenResult.serializer(),
                )
            }.map { result -> result.mapError { it.toGroupError() }.reportingUnexpected() }

    private suspend fun store(
        groupId: String,
        result: GroupsScreenResult,
    ) {
        screenResultStore.replaceGroup(
            groupId = groupId,
            groups = result.groups.map { it.toEntity() },
            tenants = result.tenants.map { it.toEntity() },
            tenantSummaries = result.tenantSummaries.map { it.toEntity() },
            groupSummaries = result.groupSummaries.map { it.toEntity() },
        )
    }

    private fun <T> Result<T, GroupError>.reportingUnexpected(): Result<T, GroupError> =
        onErr { error ->
            if (error is GroupError.Unexpected) telemetry.log(TAG, error.message, Severity.Error)
        }

    private companion object {
        const val TAG = "GroupRepository"
        const val GROUP_SCREEN = "groups:screen"
    }
}
