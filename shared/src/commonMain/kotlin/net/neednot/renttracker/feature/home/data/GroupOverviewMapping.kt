package net.neednot.renttracker.feature.home.data

import net.neednot.renttracker.core.data.GroupEntity
import net.neednot.renttracker.core.data.GroupSummaryEntity
import net.neednot.renttracker.feature.home.domain.GroupOverview

internal fun List<GroupEntity>.toGroupOverviews(summaries: List<GroupSummaryEntity>): List<GroupOverview> {
    val summaryByGroupId = summaries.associateBy { it.groupId }
    return mapNotNull { group -> summaryByGroupId[group.id]?.let { summary -> group.toGroupOverview(summary) } }
}

private fun GroupEntity.toGroupOverview(summary: GroupSummaryEntity): GroupOverview =
    GroupOverview(
        groupId = id,
        name = name,
        totalOwedCents = summary.totalOwedCents,
        activeTenantCount = summary.activeTenantCount.toInt(),
        lateTenantCount = summary.lateTenantCount.toInt(),
        archivedOwedCents = summary.archivedOwedCents,
    )
