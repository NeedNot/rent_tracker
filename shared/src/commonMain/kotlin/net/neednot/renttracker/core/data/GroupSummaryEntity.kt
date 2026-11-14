package net.neednot.renttracker.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "group_summaries")
data class GroupSummaryEntity(
    @PrimaryKey val groupId: String,
    val activeTenantCount: Long,
    val lateTenantCount: Long,
    val totalOwedCents: Long,
    val archivedTenantCount: Long,
    val archivedOwedCents: Long,
)
