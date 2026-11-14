package net.neednot.renttracker.feature.payments.domain

import kotlinx.datetime.LocalDate
import net.neednot.renttracker.core.data.PaymentMethod

data class DuplicateMatch(
    val kind: DuplicateKind,
    val date: LocalDate,
    val amountCents: Long,
    val method: PaymentMethod?,
    val byName: String,
    val byAccountId: String,
)
