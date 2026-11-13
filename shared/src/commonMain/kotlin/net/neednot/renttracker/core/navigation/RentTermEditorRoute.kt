package net.neednot.renttracker.core.navigation

import kotlinx.serialization.Serializable

@Serializable
data class RentTermEditorRoute(
    val tenantId: String,
    val rentTermId: String? = null,
)
