package net.neednot.renttracker.feature.members.presentation.viewmodel

import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.members.domain.MembersError
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.members_error_offline
import net.neednot.renttracker.resources.members_error_unexpected

internal fun MembersError.text(): UiText =
    when (this) {
        is MembersError.ValidationFailed -> UiText.Plain(message)
        is MembersError.EditConflict -> UiText.Plain(message)
        is MembersError.PermissionDenied -> UiText.Plain(message)
        is MembersError.NotFound -> UiText.Plain(message)
        MembersError.Offline -> UiText.Resource(Res.string.members_error_offline)
        is MembersError.Unexpected -> UiText.Resource(Res.string.members_error_unexpected)
    }
