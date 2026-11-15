package net.neednot.renttracker.feature.group.data

import kotlinx.coroutines.flow.Flow
import net.neednot.renttracker.core.data.CacheScope
import net.neednot.renttracker.core.data.FreshnessSource
import net.neednot.renttracker.core.sync.Freshness
import net.neednot.renttracker.feature.group.domain.GroupFreshnessRepository

class GroupFreshnessRepositoryImpl(
    private val freshnessSource: FreshnessSource,
) : GroupFreshnessRepository {
    override fun observeFreshness(groupId: String): Flow<Freshness> =
        freshnessSource.observe(listOf(CacheScope.group(groupId)))
}
