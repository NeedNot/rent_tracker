package net.neednot.renttracker.feature.account.domain

import kotlin.time.Instant

data class AccountProfile(
    val displayName: String,
    val email: String,
    val deletionRequestedAt: Instant?,
)
