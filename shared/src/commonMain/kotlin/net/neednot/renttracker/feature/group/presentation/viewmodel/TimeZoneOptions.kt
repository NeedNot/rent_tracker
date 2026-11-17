package net.neednot.renttracker.feature.group.presentation.viewmodel

import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.group.presentation.uistate.TimeZoneOptionUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.time_zone_alaska
import net.neednot.renttracker.resources.time_zone_arizona
import net.neednot.renttracker.resources.time_zone_central
import net.neednot.renttracker.resources.time_zone_eastern
import net.neednot.renttracker.resources.time_zone_hawaii
import net.neednot.renttracker.resources.time_zone_mountain
import net.neednot.renttracker.resources.time_zone_pacific

private val CuratedTimeZones =
    listOf(
        TimeZoneOptionUiState("America/New_York", UiText.Resource(Res.string.time_zone_eastern)),
        TimeZoneOptionUiState("America/Chicago", UiText.Resource(Res.string.time_zone_central)),
        TimeZoneOptionUiState("America/Denver", UiText.Resource(Res.string.time_zone_mountain)),
        TimeZoneOptionUiState("America/Phoenix", UiText.Resource(Res.string.time_zone_arizona)),
        TimeZoneOptionUiState("America/Los_Angeles", UiText.Resource(Res.string.time_zone_pacific)),
        TimeZoneOptionUiState("America/Anchorage", UiText.Resource(Res.string.time_zone_alaska)),
        TimeZoneOptionUiState("Pacific/Honolulu", UiText.Resource(Res.string.time_zone_hawaii)),
    )

internal fun timeZoneOptions(currentZoneId: String): List<TimeZoneOptionUiState> =
    if (CuratedTimeZones.any { it.id == currentZoneId }) {
        CuratedTimeZones
    } else {
        CuratedTimeZones + TimeZoneOptionUiState(currentZoneId, UiText.Plain(currentZoneId))
    }
