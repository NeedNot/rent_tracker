package net.neednot.renttracker.feature.group.domain

import net.neednot.renttracker.core.access.CallerAccess
import kotlin.time.Instant

data class GroupDetails(
    val groupId: String,
    val name: String,
    val settings: GroupSettings,
    val version: Long,
    val archivedAt: Instant?,
    val totalOwedCents: Long,
    val lateTenantCount: Int,
    val archivedOwedCents: Long,
    val activeTenants: List<GroupTenant>,
    val archivedTenants: List<GroupTenant>,
    val access: CallerAccess,
)
