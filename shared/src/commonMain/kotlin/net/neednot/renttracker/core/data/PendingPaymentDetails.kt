package net.neednot.renttracker.core.data

data class PendingPaymentDetails(
    val amountCents: Long,
    val dateReceived: String,
    val method: PaymentMethod?,
    val note: String?,
)
