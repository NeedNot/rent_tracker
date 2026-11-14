package net.neednot.renttracker.feature.debts.presentation.viewmodel

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import net.neednot.renttracker.core.data.DebtKind
import net.neednot.renttracker.core.designsystem.format.parseDollarsToCents
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.validation.InputLimits
import net.neednot.renttracker.core.validation.amountError
import net.neednot.renttracker.core.validation.lengthError
import net.neednot.renttracker.feature.debts.domain.ChargeDraft
import net.neednot.renttracker.feature.debts.presentation.uistate.ChargeEditorField
import net.neednot.renttracker.feature.debts.presentation.uistate.ChargeEditorUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.charge_editor_error_note_required
import net.neednot.renttracker.resources.input_error_amount_format

private val SERVER_FIELDS =
    mapOf(
        "Amount" to ChargeEditorField.Amount,
        "Note" to ChargeEditorField.Note,
        "Notes" to ChargeEditorField.Note,
    )

internal fun ChargeEditorUiState.validate(): Result<ChargeDraft, Map<ChargeEditorField, UiText>> {
    val amountCents = parseDollarsToCents(amount)
    val note = note.trim()
    val month = forMonth
    val date = date
    val errors = fieldErrors(includesMissing = true)
    if (errors.isNotEmpty() || amountCents == null || date == null) return Err(errors)
    return when {
        kind == DebtKind.Custom -> Ok(ChargeDraft.Custom(amountCents, date, note))
        month != null -> Ok(ChargeDraft.LateFee(amountCents, date, month, note.ifEmpty { null }))
        else -> Err(errors)
    }
}

internal fun ChargeEditorUiState.fieldErrors(includesMissing: Boolean): Map<ChargeEditorField, UiText> =
    buildMap {
        val amountError =
            amountError(amount, allowsZero = false)
                ?: if (includesMissing &&
                    amount.isBlank()
                ) {
                    UiText.Resource(Res.string.input_error_amount_format)
                } else {
                    null
                }
        amountError?.let { put(ChargeEditorField.Amount, it) }
        val noteError =
            lengthError(
                note,
                if (kind ==
                    DebtKind.Custom
                ) {
                    InputLimits.MAX_NAME_LENGTH
                } else {
                    InputLimits.MAX_NOTES_LENGTH
                },
            )
                ?: if (includesMissing && kind == DebtKind.Custom && note.isBlank()) {
                    UiText.Resource(Res.string.charge_editor_error_note_required)
                } else {
                    null
                }
        noteError?.let { put(ChargeEditorField.Note, it) }
    }

internal fun ChargeEditorUiState.revalidated(includesMissing: Boolean): ChargeEditorUiState =
    copy(fieldErrors = fieldErrors(includesMissing), error = null)

internal fun chargeEditorFieldOf(serverField: String?): ChargeEditorField? = SERVER_FIELDS[serverField]
