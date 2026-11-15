package net.neednot.renttracker.feature.group.presentation.uistate

import net.neednot.renttracker.core.data.PaymentOrder

data class GroupSettingsForm(
    val name: String = "",
    val timeZone: String = "",
    val paymentOrder: PaymentOrder = PaymentOrder.RentFirst,
    val graceDays: String = "",
    val lateFee: String = "",
)
