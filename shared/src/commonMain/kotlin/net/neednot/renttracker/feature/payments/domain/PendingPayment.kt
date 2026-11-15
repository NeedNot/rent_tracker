package net.neednot.renttracker.feature.payments.domain

data class PendingPayment(
    val tenantName: String,
    val details: PaymentDraft,
)
