package net.neednot.renttracker.feature.tenants.presentation.uistate

data class LedgerEntryUiState(
    val sheet: LedgerSheetUiState?,
    val dialog: LedgerDialogUiState?,
    val isWorking: Boolean,
)
