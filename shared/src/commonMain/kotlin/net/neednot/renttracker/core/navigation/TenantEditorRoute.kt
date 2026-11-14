package net.neednot.renttracker.core.navigation

import kotlinx.serialization.Serializable

@Serializable
data class TenantEditorRoute(
    val groupId: String,
    val tenantId: String? = null,
)
