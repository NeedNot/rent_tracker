package net.neednot.renttracker.feature.debts.presentation.viewmodel

import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.debts.domain.DebtError
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.charge_editor_error_offline
import net.neednot.renttracker.resources.entry_error_not_found
import net.neednot.renttracker.resources.entry_error_unexpected

internal fun DebtError.text(): UiText =
    when (this) {
        is DebtError.ValidationFailed -> UiText.Plain(message)
        is DebtError.PermissionDenied -> UiText.Plain(message)
        is DebtError.TenantArchived -> UiText.Plain(message)
        is DebtError.EditConflict -> UiText.Plain(message)
        is DebtError.PossibleDuplicate -> UiText.Resource(Res.string.entry_error_unexpected)
        DebtError.NotFound -> UiText.Resource(Res.string.entry_error_not_found)
        DebtError.Offline -> UiText.Resource(Res.string.charge_editor_error_offline)
        is DebtError.Unexpected -> UiText.Resource(Res.string.entry_error_unexpected)
    }
