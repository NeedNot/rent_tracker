package net.neednot.renttracker.core.navigation

import kotlinx.serialization.Serializable

@Serializable
data class EditPendingPaymentRoute(
    val tenantId: String,
    val clientId: String,
)
