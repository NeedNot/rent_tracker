package net.neednot.renttracker.feature.payments.domain

import kotlinx.datetime.LocalDate
import net.neednot.renttracker.core.data.PaymentMethod

data class PaymentDraft(
    val amountCents: Long,
    val dateReceived: LocalDate,
    val method: PaymentMethod?,
    val note: String?,
)
