package net.neednot.renttracker.feature.payments.presentation.viewmodel

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import net.neednot.renttracker.core.designsystem.format.formatDate
import net.neednot.renttracker.core.designsystem.format.parseDollarsToCents
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.payments.domain.LateFeeDraft
import net.neednot.renttracker.feature.payments.domain.PaymentDraft
import net.neednot.renttracker.feature.payments.presentation.uistate.PaymentEditorField
import net.neednot.renttracker.feature.payments.presentation.uistate.PaymentEditorUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.common_error_amount_positive
import net.neednot.renttracker.resources.entry_error_amount_format
import net.neednot.renttracker.resources.payment_editor_error_date_future

internal fun PaymentEditorUiState.validate(): Result<ValidPaymentForm, Map<PaymentEditorField, UiText>> {
    val errors = fieldErrorsNow()
    val form = if (errors.isEmpty()) toFormOrNull() else null
    return if (form == null) Err(errors) else Ok(form)
}

private fun PaymentEditorUiState.toFormOrNull(): ValidPaymentForm? {
    val amountCents = parseDollarsToCents(amount)
    val date = date
    val offer = lateFeeOffer?.takeIf { it.isChecked }
    val lateFee = offer?.let { checked -> parseDollarsToCents(checked.amount)?.let { LateFeeDraft(it, checked.month) } }
    val isComplete = amountCents != null && date != null && (offer == null || lateFee != null)
    return if (isComplete) {
        ValidPaymentForm(
            draft = PaymentDraft(amountCents, date, method, note.trim().ifEmpty { null }),
            lateFee = lateFee,
        )
    } else {
        null
    }
}

internal fun PaymentEditorUiState.dateError(): UiText? =
    if (today != null && date != null && date > today) {
        UiText.Resource(Res.string.payment_editor_error_date_future, listOf(formatDate(today)))
    } else {
        null
    }

private fun PaymentEditorUiState.fieldErrorsNow(): Map<PaymentEditorField, UiText> =
    buildMap {
        amountError(amount)?.let { put(PaymentEditorField.Amount, it) }
        dateError()?.let { put(PaymentEditorField.Date, it) }
        lateFeeOffer?.takeIf { it.isChecked }?.let { offer ->
            amountError(offer.amount)?.let { put(PaymentEditorField.LateFeeAmount, it) }
        }
    }

private fun amountError(text: String): UiText? =
    when (parseDollarsToCents(text)) {
        null -> UiText.Resource(Res.string.entry_error_amount_format)
        0L -> UiText.Resource(Res.string.common_error_amount_positive)
        else -> null
    }
