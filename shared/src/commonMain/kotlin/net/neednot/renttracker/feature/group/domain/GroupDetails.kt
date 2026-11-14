package net.neednot.renttracker.feature.group.domain

data class GroupDetails(
    val groupId: String,
    val name: String,
    val totalOwedCents: Long,
    val lateTenantCount: Int,
    val archivedOwedCents: Long,
    val activeTenants: List<GroupTenant>,
    val archivedTenants: List<GroupTenant>,
)
