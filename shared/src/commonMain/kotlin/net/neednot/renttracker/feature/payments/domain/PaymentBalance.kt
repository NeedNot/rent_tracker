package net.neednot.renttracker.feature.payments.domain

data class PaymentBalance(
    val owedCents: Long,
    val creditCents: Long,
)
