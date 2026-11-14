package net.neednot.renttracker.feature.tenants.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

data class LedgerSheetUiState(
    val key: String,
    val title: UiText,
    val amount: String,
    val chips: List<EntryChip>,
    val fields: List<LedgerFieldUiState>,
    val actions: List<LedgerSheetAction>,
    val editTarget: EntryEditTarget,
)
