package net.neednot.renttracker.core.ui

import net.neednot.renttracker.core.sync.Freshness
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.offline_banner
import net.neednot.renttracker.resources.offline_banner_days
import net.neednot.renttracker.resources.offline_banner_hours
import net.neednot.renttracker.resources.offline_banner_just_now
import net.neednot.renttracker.resources.offline_banner_minutes
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

fun Freshness.offlineBannerText(): UiText? =
    when (this) {
        Freshness.Online -> null
        is Freshness.Offline -> updatedAgoText()
    }

private fun Freshness.Offline.updatedAgoText(): UiText {
    val age = updatedAgo ?: return UiText.Resource(Res.string.offline_banner)
    return when {
        age < 1.minutes -> UiText.Resource(Res.string.offline_banner_just_now)
        age < 1.hours -> UiText.Resource(Res.string.offline_banner_minutes, listOf(age.inWholeMinutes))
        age < 1.days -> UiText.Resource(Res.string.offline_banner_hours, listOf(age.inWholeHours))
        else -> UiText.Resource(Res.string.offline_banner_days, listOf(age.inWholeDays))
    }
}
