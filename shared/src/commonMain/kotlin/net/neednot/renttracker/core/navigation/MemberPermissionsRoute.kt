package net.neednot.renttracker.core.navigation

import kotlinx.serialization.Serializable

@Serializable
data class MemberPermissionsRoute(
    val groupId: String,
    val membershipId: String,
)
