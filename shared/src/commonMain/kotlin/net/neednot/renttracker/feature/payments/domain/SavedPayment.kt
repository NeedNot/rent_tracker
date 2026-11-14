package net.neednot.renttracker.feature.payments.domain

data class SavedPayment(
    val paymentId: String,
    val version: Long,
    val details: PaymentDraft,
)
