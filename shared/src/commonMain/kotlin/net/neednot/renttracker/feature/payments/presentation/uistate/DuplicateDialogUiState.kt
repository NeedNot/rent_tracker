package net.neednot.renttracker.feature.payments.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

data class DuplicateDialogUiState(
    val title: UiText,
    val intro: UiText,
    val matches: List<DuplicateMatchUiState>,
    val question: UiText,
)
