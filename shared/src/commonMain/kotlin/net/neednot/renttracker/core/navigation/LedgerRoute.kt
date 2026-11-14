package net.neednot.renttracker.core.navigation

import kotlinx.serialization.Serializable

@Serializable
data class LedgerRoute(
    val tenantId: String,
)
