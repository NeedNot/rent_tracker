package net.neednot.renttracker.feature.debts.presentation.viewmodel

import kotlinx.datetime.yearMonth
import net.neednot.renttracker.core.data.DebtKind
import net.neednot.renttracker.core.designsystem.format.formatDollarsInput
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.debts.domain.ChargeDraft
import net.neednot.renttracker.feature.debts.domain.ChargeEntryOverview
import net.neednot.renttracker.feature.debts.presentation.uistate.ChargeEditorUiState
import net.neednot.renttracker.feature.debts.presentation.uistate.ChargeHeaderUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.balance_paid_up

internal fun ChargeEditorUiState.showing(
    overview: ChargeEntryOverview,
    suggestion: ChargeSuggestion?,
): ChargeEditorUiState {
    val shown =
        when (val saved = overview.charge?.draft) {
            is ChargeDraft.LateFee -> showing(saved)
            is ChargeDraft.Custom -> showing(saved)
            null -> showingNew(overview, suggestion)
        }
    return shown.copy(
        isLoading = false,
        header = overview.toHeader(),
        today = overview.today,
        fieldErrors = emptyMap(),
        duplicate = null,
    )
}

private fun ChargeEditorUiState.showing(saved: ChargeDraft.LateFee): ChargeEditorUiState =
    copy(
        kind = DebtKind.LateFee,
        amount = formatDollarsInput(saved.amountCents),
        date = saved.date,
        forMonth = saved.forMonth,
        note = saved.note.orEmpty(),
    )

private fun ChargeEditorUiState.showing(saved: ChargeDraft.Custom): ChargeEditorUiState =
    copy(
        kind = DebtKind.Custom,
        amount = formatDollarsInput(saved.amountCents),
        date = saved.date,
        forMonth = saved.date.yearMonth,
        note = saved.note,
    )

private fun ChargeEditorUiState.showingNew(
    overview: ChargeEntryOverview,
    suggestion: ChargeSuggestion?,
): ChargeEditorUiState =
    copy(
        kind = DebtKind.LateFee,
        amount = suggestion?.let { formatDollarsInput(it.amountCents) }.orEmpty(),
        date = overview.today,
        forMonth = suggestion?.forMonth ?: overview.today.yearMonth,
        isFromSuggestion = suggestion != null,
    )

private fun ChargeEntryOverview.toHeader(): ChargeHeaderUiState =
    ChargeHeaderUiState(
        tenantName = tenantName,
        groupName = groupName,
        balance = balance?.inWords() ?: UiText.Resource(Res.string.balance_paid_up),
    )
