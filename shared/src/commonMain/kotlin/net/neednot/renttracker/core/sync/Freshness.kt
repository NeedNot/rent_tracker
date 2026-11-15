package net.neednot.renttracker.core.sync

import kotlin.time.Duration

sealed interface Freshness {
    data object Online : Freshness

    data class Offline(
        val updatedAgo: Duration?,
    ) : Freshness
}
