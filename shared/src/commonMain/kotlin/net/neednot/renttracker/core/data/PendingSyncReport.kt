package net.neednot.renttracker.core.data

data class PendingSyncReport(
    val needsAttention: List<PendingPaymentEntity>,
    val retryLater: Boolean,
)
