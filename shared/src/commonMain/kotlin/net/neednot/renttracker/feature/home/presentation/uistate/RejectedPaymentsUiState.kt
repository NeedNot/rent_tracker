package net.neednot.renttracker.feature.home.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

data class RejectedPaymentsUiState(
    val summary: UiText,
    val payments: List<RejectedPaymentUiState>,
    val isSheetOpen: Boolean = false,
)
