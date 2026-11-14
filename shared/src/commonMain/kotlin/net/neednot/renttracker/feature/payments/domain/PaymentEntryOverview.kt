package net.neednot.renttracker.feature.payments.domain

import kotlinx.datetime.LocalDate
import net.neednot.renttracker.core.data.PaymentMethod

data class PaymentEntryOverview(
    val tenantName: String,
    val groupName: String,
    val balance: PaymentBalance?,
    val today: LocalDate,
    val lastPaymentMethod: PaymentMethod?,
    val lateFeeSuggestion: LateFeeSuggestion?,
    val canAddDebts: Boolean,
    val callerAccountId: String?,
    val payment: SavedPayment?,
)
