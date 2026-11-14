package net.neednot.renttracker.feature.tenants.domain

import kotlin.time.Instant

sealed interface EntryStatus {
    data object Active : EntryStatus

    data class Cancelled(
        val reason: String,
        val by: Person,
        val at: Instant,
    ) : EntryStatus

    data class Bounced(
        val by: Person,
        val at: Instant,
    ) : EntryStatus
}
