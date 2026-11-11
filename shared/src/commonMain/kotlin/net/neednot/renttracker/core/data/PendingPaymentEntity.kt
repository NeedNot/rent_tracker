package net.neednot.renttracker.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_payments")
data class PendingPaymentEntity(
    @PrimaryKey val clientId: String,
    val tenantId: String,
    val amountCents: Long,
    val dateReceived: String,
    val method: String,
    val note: String?,
    val status: PendingPaymentStatus,
)
