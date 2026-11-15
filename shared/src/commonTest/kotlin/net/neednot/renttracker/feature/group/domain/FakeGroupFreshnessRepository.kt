package net.neednot.renttracker.feature.group.domain

import kotlinx.coroutines.flow.MutableStateFlow
import net.neednot.renttracker.core.sync.Freshness

class FakeGroupFreshnessRepository : GroupFreshnessRepository {
    val freshness = MutableStateFlow<Freshness>(Freshness.Online)
    val observedGroupIds = mutableListOf<String>()

    override fun observeFreshness(groupId: String): MutableStateFlow<Freshness> {
        observedGroupIds += groupId
        return freshness
    }
}
