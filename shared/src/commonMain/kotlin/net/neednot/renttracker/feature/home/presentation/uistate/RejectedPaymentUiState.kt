package net.neednot.renttracker.feature.home.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

data class RejectedPaymentUiState(
    val clientId: String,
    val title: UiText,
    val amount: String,
    val dateReceived: UiText,
    val reason: UiText,
)
