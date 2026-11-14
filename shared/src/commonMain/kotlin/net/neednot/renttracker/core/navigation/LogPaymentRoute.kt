package net.neednot.renttracker.core.navigation

import kotlinx.serialization.Serializable

@Serializable
data class LogPaymentRoute(
    val tenantId: String,
)
