package net.neednot.renttracker.feature.tenants.domain

import kotlinx.datetime.LocalDate
import net.neednot.renttracker.core.data.PaymentMethod

data class MonthPayment(
    val paymentId: String,
    val dateReceived: LocalDate,
    val method: PaymentMethod?,
    val paymentCents: Long,
    val allocatedToMonthCents: Long,
    val otherAllocations: List<OtherAllocation>,
)
