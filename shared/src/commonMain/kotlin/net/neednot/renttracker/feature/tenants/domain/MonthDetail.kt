package net.neednot.renttracker.feature.tenants.domain

import net.neednot.renttracker.core.data.PaymentOrder

data class MonthDetail(
    val month: CalendarMonth,
    val payments: List<MonthPayment>,
    val lateFee: MonthLateFee?,
    val paymentOrder: PaymentOrder,
    val canAddCharge: Boolean,
)
