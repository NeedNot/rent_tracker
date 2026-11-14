package net.neednot.renttracker.feature.tenants.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

data class MonthLateNoticeUiState(
    val title: UiText,
    val message: UiText,
    val addFee: AddFeeUiState?,
)
