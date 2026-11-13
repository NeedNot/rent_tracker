package net.neednot.renttracker.core.data

import kotlinx.coroutines.flow.Flow

interface AsOfClock {
    /** The current time in epoch milliseconds, rounded down to the hour; emits again each time the hour rolls over. */
    val asOf: Flow<Long>
}
