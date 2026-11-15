package net.neednot.renttracker.feature.group.domain

import kotlinx.coroutines.flow.Flow
import net.neednot.renttracker.core.sync.Freshness

interface GroupFreshnessRepository {
    fun observeFreshness(groupId: String): Flow<Freshness>
}
