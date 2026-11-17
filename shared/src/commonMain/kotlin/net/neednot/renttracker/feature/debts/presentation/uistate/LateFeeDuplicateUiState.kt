package net.neednot.renttracker.feature.debts.presentation.uistate

data class LateFeeDuplicateUiState(
    val month: String,
    val matches: List<DuplicateLateFeeUiState>,
)
