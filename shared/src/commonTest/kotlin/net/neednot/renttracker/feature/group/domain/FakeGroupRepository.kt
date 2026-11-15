package net.neednot.renttracker.feature.group.domain

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.MutableStateFlow
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced

class FakeGroupRepository : GroupRepository {
    val group = MutableStateFlow<Synced<GroupDetails?, GroupError>>(Synced(null, SyncStatus.Syncing))
    val observedGroupIds = mutableListOf<String>()

    var updateSettingsResult: Result<Unit, GroupError> = Ok(Unit)
    var archiveResult: Result<Unit, GroupError> = Ok(Unit)
    var unarchiveResult: Result<Unit, GroupError> = Ok(Unit)
    var leaveResult: Result<Unit, GroupError> = Ok(Unit)

    val updatedSettings = mutableListOf<Triple<String, Long, GroupSettings>>()
    val archivedGroupIds = mutableListOf<String>()
    val unarchivedGroupIds = mutableListOf<String>()
    val leftGroupIds = mutableListOf<String>()

    override fun observeGroup(groupId: String): MutableStateFlow<Synced<GroupDetails?, GroupError>> {
        observedGroupIds += groupId
        return group
    }

    override suspend fun updateSettings(
        groupId: String,
        expectedVersion: Long,
        settings: GroupSettings,
    ): Result<Unit, GroupError> {
        updatedSettings += Triple(groupId, expectedVersion, settings)
        return updateSettingsResult
    }

    override suspend fun archiveGroup(groupId: String): Result<Unit, GroupError> {
        archivedGroupIds += groupId
        return archiveResult
    }

    override suspend fun unarchiveGroup(groupId: String): Result<Unit, GroupError> {
        unarchivedGroupIds += groupId
        return unarchiveResult
    }

    override suspend fun leaveGroup(groupId: String): Result<Unit, GroupError> {
        leftGroupIds += groupId
        return leaveResult
    }
}
