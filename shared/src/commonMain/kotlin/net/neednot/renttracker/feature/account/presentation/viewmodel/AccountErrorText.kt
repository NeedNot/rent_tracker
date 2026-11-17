package net.neednot.renttracker.feature.account.presentation.viewmodel

import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.account.domain.AccountError
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.account_error_load_unexpected
import net.neednot.renttracker.resources.account_error_offline
import net.neednot.renttracker.resources.account_error_session_ended
import net.neednot.renttracker.resources.account_error_unexpected

internal fun AccountError.text(): UiText =
    when (this) {
        is AccountError.ValidationFailed -> UiText.Plain(message)
        AccountError.NotSignedIn -> UiText.Resource(Res.string.account_error_session_ended)
        AccountError.Offline -> UiText.Resource(Res.string.account_error_offline)
        is AccountError.Unexpected -> UiText.Resource(Res.string.account_error_unexpected)
    }

internal fun AccountError.loadText(): UiText =
    if (this is AccountError.Unexpected) UiText.Resource(Res.string.account_error_load_unexpected) else text()
