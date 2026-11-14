package net.neednot.renttracker.feature.tenants.presentation.components

import net.neednot.renttracker.feature.tenants.presentation.uistate.AddFeeUiState

internal data class CalendarActions(
    val onPreviousYearClicked: () -> Unit,
    val onNextYearClicked: () -> Unit,
    val onMonthClicked: (month: String) -> Unit,
    val onMonthDetailDismissed: () -> Unit,
    val onAddFeeClicked: (AddFeeUiState) -> Unit,
) {
    companion object {
        val None = CalendarActions({}, {}, {}, {}, {})
    }
}
