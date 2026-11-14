package net.neednot.renttracker.feature.payments.presentation.viewmodel

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import net.neednot.renttracker.core.designsystem.format.formatDate
import net.neednot.renttracker.core.designsystem.format.parseDollarsToCents
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.validation.InputLimits
import net.neednot.renttracker.core.validation.amountError
import net.neednot.renttracker.core.validation.lengthError
import net.neednot.renttracker.feature.payments.domain.LateFeeDraft
import net.neednot.renttracker.feature.payments.domain.PaymentDraft
import net.neednot.renttracker.feature.payments.presentation.uistate.PaymentEditorField
import net.neednot.renttracker.feature.payments.presentation.uistate.PaymentEditorUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.input_error_amount_format
import net.neednot.renttracker.resources.payment_editor_error_date_future

private val SERVER_FIELDS =
    mapOf(
        "Amount" to PaymentEditorField.Amount,
        "Date received" to PaymentEditorField.Date,
        "Notes" to PaymentEditorField.Note,
    )

internal fun PaymentEditorUiState.validate(): Result<ValidPaymentForm, Map<PaymentEditorField, UiText>> {
    val errors = fieldErrors(includesMissing = true)
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

internal fun PaymentEditorUiState.fieldErrors(includesMissing: Boolean): Map<PaymentEditorField, UiText> =
    buildMap {
        amountFieldError(amount, includesMissing)?.let { put(PaymentEditorField.Amount, it) }
        dateError()?.let { put(PaymentEditorField.Date, it) }
        lengthError(note, InputLimits.MAX_NOTES_LENGTH)?.let { put(PaymentEditorField.Note, it) }
        lateFeeOffer?.takeIf { it.isChecked }?.let { offer ->
            amountFieldError(offer.amount, includesMissing)?.let { put(PaymentEditorField.LateFeeAmount, it) }
        }
    }

internal fun PaymentEditorUiState.revalidated(includesMissing: Boolean): PaymentEditorUiState =
    copy(fieldErrors = fieldErrors(includesMissing), error = null)

internal fun paymentEditorFieldOf(serverField: String?): PaymentEditorField? = SERVER_FIELDS[serverField]

private fun amountFieldError(
    text: String,
    includesMissing: Boolean,
): UiText? =
    amountError(text, allowsZero = false)
        ?: if (includesMissing && text.isBlank()) UiText.Resource(Res.string.input_error_amount_format) else null
