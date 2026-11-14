package net.neednot.renttracker.feature.tenants.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

data class CalendarUiState(
    val year: String,
    val squares: List<CalendarSquareUiState>,
    val error: UiText? = null,
)
