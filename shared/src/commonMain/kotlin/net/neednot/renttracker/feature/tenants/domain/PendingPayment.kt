package net.neednot.renttracker.feature.tenants.domain

import kotlinx.datetime.LocalDate
import net.neednot.renttracker.core.data.PaymentMethod

data class PendingPayment(
    val clientId: String,
    val tenantName: String,
    val amountCents: Long,
    val dateReceived: LocalDate,
    val method: PaymentMethod?,
    val state: PendingState,
)
