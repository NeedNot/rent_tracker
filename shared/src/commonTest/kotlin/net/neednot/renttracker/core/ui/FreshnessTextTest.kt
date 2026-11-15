package net.neednot.renttracker.core.ui

import net.neednot.renttracker.core.sync.Freshness
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.offline_banner
import net.neednot.renttracker.resources.offline_banner_days
import net.neednot.renttracker.resources.offline_banner_hours
import net.neednot.renttracker.resources.offline_banner_just_now
import net.neednot.renttracker.resources.offline_banner_minutes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class FreshnessTextTest {
    @Test
    fun onlineShowsNoBanner() {
        assertNull(Freshness.Online.offlineBannerText())
    }

    @Test
    fun theBannerSaysHowOldTheDataIsInTheLargestWholeUnit() {
        assertEquals(UiText.Resource(Res.string.offline_banner_just_now), offline(30.seconds))
        assertEquals(UiText.Resource(Res.string.offline_banner_minutes, listOf(40L)), offline(40.minutes))
        assertEquals(UiText.Resource(Res.string.offline_banner_hours, listOf(2L)), offline(2.hours + 59.minutes))
        assertEquals(UiText.Resource(Res.string.offline_banner_days, listOf(3L)), offline(3.days))
    }

    @Test
    fun dataThatWasNeverSyncedSaysOnlyOffline() {
        assertEquals(
            UiText.Resource(Res.string.offline_banner),
            Freshness.Offline(updatedAgo = null).offlineBannerText(),
        )
    }

    private fun offline(age: kotlin.time.Duration) = Freshness.Offline(age).offlineBannerText()
}
