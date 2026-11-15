package net.neednot.renttracker.feature.tenants.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

sealed interface PendingSheetBody {
    data class Waiting(
        val message: UiText,
        val fields: List<LedgerFieldUiState>,
    ) : PendingSheetBody

    data class PossibleDuplicate(
        val title: UiText,
        val intro: UiText,
        val matches: List<PendingMatchUiState>,
    ) : PendingSheetBody

    data class TenantChanged(
        val title: UiText,
        val intro: UiText,
        val changes: List<PendingChangeUiState>,
    ) : PendingSheetBody

    data class Rejected(
        val reason: UiText,
        val notSaved: UiText,
    ) : PendingSheetBody
}
