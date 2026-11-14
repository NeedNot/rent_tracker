package net.neednot.renttracker.feature.debts.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

data class LateFeeDuplicateUiState(
    val intro: UiText,
    val matches: List<DuplicateLateFeeUiState>,
)
