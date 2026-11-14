package net.neednot.renttracker.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tenant_summaries")
data class TenantSummaryEntity(
    @PrimaryKey val tenantId: String,
    val state: TenantBalanceState,
    val rentOwedCents: Long,
    val lateFeesOwedCents: Long,
    val otherOwedCents: Long,
    val creditCents: Long,
    val nextDueDate: String?,
    val partialMoveOutStart: String?,
    val partialMoveOutEnd: String?,
)
