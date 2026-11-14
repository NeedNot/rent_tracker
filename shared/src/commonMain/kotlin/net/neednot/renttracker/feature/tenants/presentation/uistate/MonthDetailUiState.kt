package net.neednot.renttracker.feature.tenants.presentation.uistate

import net.neednot.renttracker.core.designsystem.components.CalendarState
import net.neednot.renttracker.core.ui.UiText

data class MonthDetailUiState(
    val title: String,
    val state: CalendarState,
    val stateLabel: UiText,
    val rentLabel: UiText,
    val rent: String,
    val payments: List<MonthPaymentUiState>,
    val coverageNote: UiText?,
    val totals: List<BalanceLineUiState>,
    val lateNotice: MonthLateNoticeUiState?,
    val lateFee: MonthLateFeeUiState?,
)
