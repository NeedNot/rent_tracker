package net.neednot.renttracker.feature.group.presentation.viewmodel

import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.group.domain.GroupError
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.group_error_not_found
import net.neednot.renttracker.resources.group_error_offline
import net.neednot.renttracker.resources.group_error_unexpected

internal fun GroupError.text(): UiText =
    when (this) {
        GroupError.NotFound -> UiText.Resource(Res.string.group_error_not_found)
        GroupError.Offline -> UiText.Resource(Res.string.group_error_offline)
        is GroupError.Unexpected -> UiText.Resource(Res.string.group_error_unexpected)
    }
