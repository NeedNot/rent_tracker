package net.neednot.renttracker.core.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Clock

class HourlyAsOfClock(
    private val clock: Clock,
) : AsOfClock {
    override val asOf: Flow<Long> =
        flow {
            while (true) {
                val now = clock.now().toEpochMilliseconds()
                val hourStart = now - now % MILLIS_PER_HOUR
                emit(hourStart)
                delay(hourStart + MILLIS_PER_HOUR - now)
            }
        }

    private companion object {
        const val MILLIS_PER_HOUR = 3_600_000L
    }
}
