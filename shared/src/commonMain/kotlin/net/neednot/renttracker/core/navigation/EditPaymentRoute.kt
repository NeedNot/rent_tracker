package net.neednot.renttracker.core.navigation

import kotlinx.serialization.Serializable

@Serializable
data class EditPaymentRoute(
    val tenantId: String,
    val paymentId: String,
)
