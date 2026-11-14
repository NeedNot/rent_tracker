package net.neednot.renttracker.feature.tenants.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

data class CalendarUiState(
    val year: String,
    val hasPreviousYear: Boolean,
    val squares: List<CalendarSquareUiState>,
    val error: UiText? = null,
)
