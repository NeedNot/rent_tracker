package net.neednot.renttracker.feature.group.domain

import kotlinx.coroutines.flow.Flow
import net.neednot.renttracker.core.sync.Synced

interface GroupRepository {
    fun observeGroup(groupId: String): Flow<Synced<GroupDetails?, GroupError>>
}
