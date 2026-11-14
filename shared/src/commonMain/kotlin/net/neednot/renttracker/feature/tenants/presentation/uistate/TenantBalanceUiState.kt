package net.neednot.renttracker.feature.tenants.presentation.uistate

import net.neednot.renttracker.core.data.TenantBalanceState
import net.neednot.renttracker.core.ui.UiText

data class TenantBalanceUiState(
    val state: TenantBalanceState,
    val total: UiText,
    val lines: List<BalanceLineUiState>,
    val nextDue: UiText?,
    val partialMoveOutWarning: UiText?,
)
