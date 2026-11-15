package net.neednot.renttracker.feature.home.data

import net.neednot.renttracker.core.data.GroupEntity
import net.neednot.renttracker.core.data.GroupSummaryEntity
import net.neednot.renttracker.feature.home.domain.ArchivedGroup
import kotlin.time.Instant

internal fun List<GroupEntity>.toArchivedGroups(summaries: List<GroupSummaryEntity>): List<ArchivedGroup> {
    val summaryByGroupId = summaries.associateBy { it.groupId }
    return mapNotNull { group ->
        val archivedAt = group.archivedAt ?: return@mapNotNull null
        summaryByGroupId[group.id]?.let { summary ->
            ArchivedGroup(
                groupId = group.id,
                name = group.name,
                totalOwedCents = summary.totalOwedCents,
                tenantCount = summary.activeTenantCount.toInt(),
                archivedAt = Instant.fromEpochMilliseconds(archivedAt),
            )
        }
    }
}
