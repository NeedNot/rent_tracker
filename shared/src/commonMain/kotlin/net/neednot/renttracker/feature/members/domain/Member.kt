package net.neednot.renttracker.feature.members.domain

import net.neednot.renttracker.core.data.MembershipRole
import net.neednot.renttracker.core.data.PermissionSet

data class Member(
    val membershipId: String,
    val accountId: String,
    val displayName: String,
    val email: String?,
    val role: MembershipRole,
    val permissions: PermissionSet,
    val version: Long,
)
