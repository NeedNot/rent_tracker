package net.neednot.renttracker.feature.payments.presentation.viewmodel

import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.payments.domain.PaymentError
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.entry_error_not_found
import net.neednot.renttracker.resources.entry_error_unexpected
import net.neednot.renttracker.resources.payment_editor_error_offline

internal fun PaymentError.text(): UiText =
    when (this) {
        is PaymentError.ValidationFailed -> UiText.Plain(message)
        is PaymentError.PermissionDenied -> UiText.Plain(message)
        is PaymentError.TenantArchived -> UiText.Plain(message)
        is PaymentError.EditConflict -> UiText.Plain(message)
        is PaymentError.PossibleDuplicate -> UiText.Resource(Res.string.entry_error_unexpected)
        PaymentError.NotFound -> UiText.Resource(Res.string.entry_error_not_found)
        PaymentError.Offline -> UiText.Resource(Res.string.payment_editor_error_offline)
        is PaymentError.Unexpected -> UiText.Resource(Res.string.entry_error_unexpected)
    }
