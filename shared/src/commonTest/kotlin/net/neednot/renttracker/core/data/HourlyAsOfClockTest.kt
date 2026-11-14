package net.neednot.renttracker.core.data

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class HourlyAsOfClockTest {
    @Test
    fun startsAtTheCurrentTimeRoundedDownToTheHour() =
        runTest {
            val clock = HourlyAsOfClock(clockStartingAt(TEN_O_CLOCK + FORTY_MINUTES))

            clock.asOf.test {
                assertEquals(TEN_O_CLOCK, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun emitsAgainEachTimeTheHourRollsOver() =
        runTest {
            val clock = HourlyAsOfClock(clockStartingAt(TEN_O_CLOCK + FORTY_MINUTES))

            clock.asOf.test {
                assertEquals(TEN_O_CLOCK, awaitItem())
                assertEquals(TEN_O_CLOCK + HOUR, awaitItem())
                assertEquals(HOUR - FORTY_MINUTES, currentTime)
                assertEquals(TEN_O_CLOCK + 2 * HOUR, awaitItem())
                assertEquals(2 * HOUR - FORTY_MINUTES, currentTime)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun aClockStartedExactlyOnTheHourWaitsAWholeHour() =
        runTest {
            val clock = HourlyAsOfClock(clockStartingAt(TEN_O_CLOCK))

            clock.asOf.test {
                assertEquals(TEN_O_CLOCK, awaitItem())
                assertEquals(TEN_O_CLOCK + HOUR, awaitItem())
                assertEquals(HOUR, currentTime)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun TestScope.clockStartingAt(startMillis: Long): Clock =
        object : Clock {
            override fun now(): Instant = Instant.fromEpochMilliseconds(startMillis + currentTime)
        }

    private companion object {
        const val HOUR = 3_600_000L
        const val FORTY_MINUTES = 2_400_000L
        const val TEN_O_CLOCK = 1_794_736_800_000L
    }
}
