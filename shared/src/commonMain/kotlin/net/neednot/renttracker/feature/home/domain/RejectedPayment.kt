package net.neednot.renttracker.feature.home.domain

import kotlinx.datetime.LocalDate

data class RejectedPayment(
    val clientId: String,
    val tenantName: String,
    val amountCents: Long,
    val dateReceived: LocalDate,
    val reason: RejectedPaymentReason,
    val message: String?,
)
