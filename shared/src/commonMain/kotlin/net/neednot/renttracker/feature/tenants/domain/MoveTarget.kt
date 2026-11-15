package net.neednot.renttracker.feature.tenants.domain

import net.neednot.renttracker.core.data.PaymentOrder

data class MoveTarget(
    val groupId: String,
    val name: String,
    val tenantCount: Int,
    val paymentOrder: PaymentOrder,
    val graceDays: Int,
)
