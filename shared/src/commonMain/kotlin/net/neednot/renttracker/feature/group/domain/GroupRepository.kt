package net.neednot.renttracker.feature.group.domain

import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.Flow
import net.neednot.renttracker.core.sync.Synced

interface GroupRepository {
    fun observeGroup(groupId: String): Flow<Synced<GroupDetails?, GroupError>>

    suspend fun updateSettings(
        groupId: String,
        expectedVersion: Long,
        settings: GroupSettings,
    ): Result<Unit, GroupError>

    suspend fun archiveGroup(groupId: String): Result<Unit, GroupError>

    suspend fun unarchiveGroup(groupId: String): Result<Unit, GroupError>

    suspend fun leaveGroup(groupId: String): Result<Unit, GroupError>
}
