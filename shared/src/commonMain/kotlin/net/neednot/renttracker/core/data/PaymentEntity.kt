package net.neednot.renttracker.core.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "payments", indices = [Index("tenantId")])
data class PaymentEntity(
    @PrimaryKey val id: String,
    val creationTime: Long,
    val tenantId: String,
    val amountCents: Long,
    val dateReceived: String,
    val method: PaymentMethod?,
    val note: String?,
    val loggedBy: String,
    val bouncedAt: Long?,
    val bouncedBy: String?,
    val deletedAt: Long?,
    val deletedBy: String?,
    val deleteReason: String?,
    val lastEditedAt: Long?,
    val lastEditedBy: String?,
    val version: Long,
)
