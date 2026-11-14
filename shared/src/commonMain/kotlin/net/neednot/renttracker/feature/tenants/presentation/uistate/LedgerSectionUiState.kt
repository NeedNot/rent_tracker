package net.neednot.renttracker.feature.tenants.presentation.uistate

data class LedgerSectionUiState(
    val header: String,
    val rows: List<LedgerRowUiState>,
)
