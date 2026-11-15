package net.neednot.renttracker.feature.tenants.data

import net.neednot.renttracker.core.data.toEntity
import net.neednot.renttracker.feature.tenants.domain.MoveTarget
import net.neednot.renttracker.model.tenants.TenantsMoveTargetsResult

internal fun TenantsMoveTargetsResult.toMoveTargets(): List<MoveTarget> {
    val summaryByGroupId = groupSummaries.associateBy { it.groupId }
    return groups.map { it.toEntity() }.map { group ->
        MoveTarget(
            groupId = group.id,
            name = group.name,
            tenantCount = summaryByGroupId[group.id]?.activeTenantCount?.toInt() ?: 0,
            paymentOrder = group.paymentOrder,
            graceDays = group.graceDays.toInt(),
        )
    }
}
