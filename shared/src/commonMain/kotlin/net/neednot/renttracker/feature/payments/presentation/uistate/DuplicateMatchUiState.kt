package net.neednot.renttracker.feature.payments.presentation.uistate

import net.neednot.renttracker.core.data.PaymentMethod
import net.neednot.renttracker.core.ui.UiText

data class DuplicateMatchUiState(
    val kind: UiText?,
    val date: String,
    val method: PaymentMethod?,
    val by: UiText,
    val amount: String,
)
