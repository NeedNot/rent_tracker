package net.neednot.renttracker.feature.group.domain

import net.neednot.renttracker.core.data.PaymentOrder

data class GroupSettings(
    val name: String,
    val timeZone: String,
    val paymentOrder: PaymentOrder,
    val graceDays: Int,
    val lateFeeFlatCents: Long?,
)
