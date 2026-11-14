package net.neednot.renttracker.core.data

import androidx.room.Entity

@Entity(tableName = "allocations", primaryKeys = ["tenantId", "key"])
data class AllocationEntity(
    val tenantId: String,
    val key: String,
    val paymentId: String,
    val chargeKind: AllocationChargeKind,
    val month: String?,
    val debtId: String?,
    val amountCents: Long,
)
