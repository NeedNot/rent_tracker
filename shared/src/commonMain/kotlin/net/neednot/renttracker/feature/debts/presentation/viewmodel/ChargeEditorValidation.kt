package net.neednot.renttracker.feature.debts.presentation.viewmodel

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import net.neednot.renttracker.core.data.DebtKind
import net.neednot.renttracker.core.designsystem.format.parseDollarsToCents
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.debts.domain.ChargeDraft
import net.neednot.renttracker.feature.debts.presentation.uistate.ChargeEditorField
import net.neednot.renttracker.feature.debts.presentation.uistate.ChargeEditorUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.charge_editor_error_note_required
import net.neednot.renttracker.resources.common_error_amount_positive
import net.neednot.renttracker.resources.entry_error_amount_format

internal fun ChargeEditorUiState.validate(): Result<ChargeDraft, Map<ChargeEditorField, UiText>> {
    val amountCents = parseDollarsToCents(amount)?.takeIf { it > 0 }
    val note = note.trim()
    val month = forMonth
    val date = date
    val errors = fieldErrorsNow()
    if (errors.isNotEmpty() || amountCents == null || date == null) return Err(errors)
    return when {
        kind == DebtKind.Custom -> Ok(ChargeDraft.Custom(amountCents, date, note))
        month != null -> Ok(ChargeDraft.LateFee(amountCents, date, month, note.ifEmpty { null }))
        else -> Err(errors)
    }
}

private fun ChargeEditorUiState.fieldErrorsNow(): Map<ChargeEditorField, UiText> =
    buildMap {
        amountError(amount)?.let { put(ChargeEditorField.Amount, it) }
        if (kind == DebtKind.Custom && note.isBlank()) {
            put(ChargeEditorField.Note, UiText.Resource(Res.string.charge_editor_error_note_required))
        }
    }

private fun amountError(text: String): UiText? =
    when (parseDollarsToCents(text)) {
        null -> UiText.Resource(Res.string.entry_error_amount_format)
        0L -> UiText.Resource(Res.string.common_error_amount_positive)
        else -> null
    }
