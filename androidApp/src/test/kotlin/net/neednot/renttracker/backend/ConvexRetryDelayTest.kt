package net.neednot.renttracker.backend

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class ConvexRetryDelayTest {
    @Test
    fun retriesStartAtTwoSecondsAndDoubleUpToAMinute() {
        assertEquals(
            listOf(2.seconds, 4.seconds, 8.seconds, 16.seconds, 32.seconds, 60.seconds, 60.seconds),
            (0L..6L).map(::convexRetryDelay),
        )
    }
}
