package net.neednot.renttracker.feature.tenants.presentation.uistate

import net.neednot.renttracker.core.designsystem.components.CalendarState
import net.neednot.renttracker.core.ui.UiText

data class CalendarSquareUiState(
    val month: String,
    val monthName: String,
    val state: CalendarState,
    val stateLabel: UiText,
    val amount: UiText?,
    val feeMarker: UiText?,
    val description: List<UiText>,
    val isTappable: Boolean,
    val isCurrentMonth: Boolean = false,
)
