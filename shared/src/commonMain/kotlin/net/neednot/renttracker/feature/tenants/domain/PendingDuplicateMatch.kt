package net.neednot.renttracker.feature.tenants.domain

import kotlinx.datetime.LocalDate
import net.neednot.renttracker.core.data.DuplicateMatchKind
import net.neednot.renttracker.core.data.PaymentMethod

data class PendingDuplicateMatch(
    val kind: DuplicateMatchKind,
    val date: LocalDate,
    val amountCents: Long,
    val method: PaymentMethod?,
    val byName: String,
)
