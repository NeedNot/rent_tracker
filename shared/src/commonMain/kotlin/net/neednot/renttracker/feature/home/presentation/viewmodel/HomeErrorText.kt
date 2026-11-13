package net.neednot.renttracker.feature.home.presentation.viewmodel

import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.home.domain.HomeError
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.home_error_offline
import net.neednot.renttracker.resources.home_error_unexpected

internal fun HomeError.text(): UiText =
    when (this) {
        is HomeError.ValidationFailed -> UiText.Plain(message)
        HomeError.Offline -> UiText.Resource(Res.string.home_error_offline)
        is HomeError.Unexpected -> UiText.Resource(Res.string.home_error_unexpected)
    }
