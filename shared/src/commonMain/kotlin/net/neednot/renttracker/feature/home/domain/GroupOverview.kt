package net.neednot.renttracker.feature.home.domain

data class GroupOverview(
    val groupId: String,
    val name: String,
    val totalOwedCents: Long,
    val activeTenantCount: Int,
    val lateTenantCount: Int,
    val archivedOwedCents: Long,
)
