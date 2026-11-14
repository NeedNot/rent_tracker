package net.neednot.renttracker.feature.tenants.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

data class LateFeeSuggestionUiState(
    val message: UiText,
    val month: String,
    val cents: Long,
)
