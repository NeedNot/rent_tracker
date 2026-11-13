package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import kotlinx.datetime.plusMonth
import kotlinx.datetime.yearMonth
import net.neednot.renttracker.core.designsystem.format.formatDollarsInput
import net.neednot.renttracker.core.designsystem.format.parseDollarsToCents
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.tenants.domain.RentTermDraft
import net.neednot.renttracker.feature.tenants.domain.TenantOverview
import net.neednot.renttracker.feature.tenants.presentation.uistate.RentTermEditorField
import net.neednot.renttracker.feature.tenants.presentation.uistate.RentTermEditorUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.tenant_editor_error_due_day_format

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
    val amountCents = parseDollarsToCents(amount)?.takeIf { it > 0 }
    val dueDay = dueDay.trim().toIntOrNull()
    return if (startMonth != null && amountCents != null && dueDay != null) {
        RentTermDraft(startMonth = startMonth, amountCents = amountCents, dueDay = dueDay)
    } else {
        null
    }
}

internal fun RentTermEditorUiState.formatErrors(): Map<RentTermEditorField, UiText> =
    buildMap {
        positiveAmountFormatError(amount)?.let { put(RentTermEditorField.Amount, it) }
        if (dueDay.trim().toIntOrNull() == null) {
            put(RentTermEditorField.DueDay, UiText.Resource(Res.string.tenant_editor_error_due_day_format))
        }
    }
