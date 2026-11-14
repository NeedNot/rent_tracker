package net.neednot.renttracker.feature.group.domain

import kotlinx.coroutines.flow.MutableStateFlow
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced

class FakeGroupRepository : GroupRepository {
    val group = MutableStateFlow<Synced<GroupDetails?, GroupError>>(Synced(null, SyncStatus.Syncing))
    val observedGroupIds = mutableListOf<String>()

    override fun observeGroup(groupId: String): MutableStateFlow<Synced<GroupDetails?, GroupError>> {
        observedGroupIds += groupId
        return group
    }
}
