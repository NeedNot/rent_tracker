package net.neednot.renttracker.core.navigation

import kotlinx.serialization.Serializable

@Serializable
data class EditChargeRoute(
    val tenantId: String,
    val debtId: String,
)
