package net.neednot.renttracker.core.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "pending_payments", indices = [Index("tenantId"), Index("sequence", unique = true)])
data class PendingPaymentEntity(
    @PrimaryKey val clientId: String,
    val sequence: Long,
    val groupId: String,
    val tenantId: String,
    val tenantName: String,
    val amountCents: Long,
    val dateReceived: String,
    val method: PaymentMethod?,
    val note: String?,
    val status: PendingPaymentStatus,
    val confirmationReason: PendingConfirmationReason?,
    val rejectionReason: PendingRejectionReason?,
    val rejectionMessage: String?,
    val duplicateConfirmed: Boolean,
    val tenantFingerprint: String,
    val balanceCentsWhenLogged: Long?,
)
