package net.neednot.renttracker.feature.tenants.domain

import kotlinx.datetime.LocalDate
import net.neednot.renttracker.core.data.PaymentMethod
import kotlin.time.Instant

data class PaymentDetails(
    val paymentId: String,
    val version: Long,
    val method: PaymentMethod?,
    val note: String?,
    val dateReceived: LocalDate,
    val loggedAt: Instant,
    val loggedBy: Person,
    val status: EntryStatus,
    val edit: EntryEdit?,
    val canChange: Boolean,
)
