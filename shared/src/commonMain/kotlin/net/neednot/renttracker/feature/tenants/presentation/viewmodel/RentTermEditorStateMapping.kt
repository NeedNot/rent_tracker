package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import kotlinx.datetime.plusMonth
import kotlinx.datetime.yearMonth
import net.neednot.renttracker.core.designsystem.format.formatDollarsInput
import net.neednot.renttracker.core.designsystem.format.parseDollarsToCents
import net.neednot.renttracker.core.designsystem.format.positiveAmountFormatError
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.validation.InputLimits
import net.neednot.renttracker.core.validation.amountError
import net.neednot.renttracker.core.validation.lengthError
import net.neednot.renttracker.core.validation.wholeNumberError
import net.neednot.renttracker.feature.tenants.domain.RentTermDraft
import net.neednot.renttracker.feature.tenants.domain.TenantOverview
import net.neednot.renttracker.feature.tenants.presentation.uistate.RentTermEditorField
import net.neednot.renttracker.feature.tenants.presentation.uistate.RentTermEditorUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.input_error_amount_format
import net.neednot.renttracker.resources.tenant_editor_error_due_day

internal fun TenantOverview.loadRentTerm(rentTermId: String?): LoadedRentTerm? =
    if (rentTermId == null) newRentTerm() else savedRentTerm(rentTermId)

private fun TenantOverview.newRentTerm(): LoadedRentTerm? {
    val currentMonth = today.yearMonth
    val termInEffect = activeRentTerms.lastOrNull { it.startMonth <= currentMonth } ?: activeRentTerms.firstOrNull()
    return termInEffect?.let { term ->
        LoadedRentTerm(
            currentMonth = currentMonth,
            startMonth = currentMonth.plusMonth(),
            amount = "",
            dueDay = term.dueDay,
            expectedVersion = null,
            canDelete = false,
        )
    }
}

private fun TenantOverview.savedRentTerm(rentTermId: String): LoadedRentTerm? =
    activeRentTerms.find { it.rentTermId == rentTermId }?.let { term ->
        LoadedRentTerm(
            currentMonth = today.yearMonth,
            startMonth = term.startMonth,
            amount = formatDollarsInput(term.amountCents),
            dueDay = term.dueDay,
            expectedVersion = term.version,
            canDelete = term != activeRentTerms.first(),
        )
    }

internal fun RentTermEditorUiState.draftOrNull(): RentTermDraft? {
    if (fieldErrors(includesMissing = true).isNotEmpty()) return null
    val amountCents = parseDollarsToCents(amount)
    val dueDay = dueDay.trim().toIntOrNull()
    return if (startMonth != null && amountCents != null && dueDay != null) {
        RentTermDraft(startMonth = startMonth, amountCents = amountCents, dueDay = dueDay)
    } else {
        null
    }
}

internal fun RentTermEditorUiState.fieldErrors(includesMissing: Boolean): Map<RentTermEditorField, UiText> =
    buildMap {
        val amountMissing = UiText.Resource(Res.string.input_error_amount_format)
        val dueDayError = UiText.Resource(Res.string.tenant_editor_error_due_day)
        (amountError(amount, allowsZero = false) ?: amountMissing.takeIf { includesMissing && amount.isBlank() })
            ?.let { put(RentTermEditorField.Amount, it) }
        (
            wholeNumberError(dueDay, InputLimits.DUE_DAYS, Res.string.tenant_editor_error_due_day)
                ?: dueDayError.takeIf { includesMissing && dueDay.isBlank() }
        )?.let { put(RentTermEditorField.DueDay, it) }
    }

internal fun RentTermEditorUiState.withFieldErrors(includesMissing: Boolean): RentTermEditorUiState =
    copy(fieldErrors = fieldErrors(includesMissing), error = null)

internal fun RentTermEditorUiState.withDeleteReason(reason: String): RentTermEditorUiState =
    copy(deletion = deletion?.copy(reason = reason, error = lengthError(reason, InputLimits.MAX_NAME_LENGTH)))

internal fun rentTermEditorFieldOf(serverField: String?): RentTermEditorField? = SERVER_FIELDS[serverField]

private val SERVER_FIELDS =
    mapOf(
        "Start month" to RentTermEditorField.StartMonth,
        "Rent" to RentTermEditorField.Amount,
        "Due day" to RentTermEditorField.DueDay,
    )
