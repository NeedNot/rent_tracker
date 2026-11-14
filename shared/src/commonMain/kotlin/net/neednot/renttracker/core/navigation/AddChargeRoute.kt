package net.neednot.renttracker.core.navigation

import kotlinx.serialization.Serializable

@Serializable
data class AddChargeRoute(
    val tenantId: String,
    val suggestedMonth: String? = null,
    val suggestedCents: Long? = null,
)
