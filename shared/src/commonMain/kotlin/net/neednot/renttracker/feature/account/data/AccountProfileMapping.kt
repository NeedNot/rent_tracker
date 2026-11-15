package net.neednot.renttracker.feature.account.data

import net.neednot.renttracker.core.data.AccountProfileEntity
import net.neednot.renttracker.feature.account.domain.AccountProfile
import kotlin.time.Instant

internal fun AccountProfileEntity.toAccountProfile(): AccountProfile =
    AccountProfile(
        displayName = displayName,
        email = email,
        deletionRequestedAt = deletionRequestedAt?.let(Instant::fromEpochMilliseconds),
    )
