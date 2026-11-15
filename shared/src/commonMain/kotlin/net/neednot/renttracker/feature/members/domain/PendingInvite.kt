package net.neednot.renttracker.feature.members.domain

import net.neednot.renttracker.core.data.PermissionSet
import kotlin.time.Instant

data class PendingInvite(
    val inviteId: String,
    val email: String,
    val permissions: PermissionSet,
    val expiresAt: Instant,
)
