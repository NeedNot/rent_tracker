package net.neednot.renttracker.feature.payments.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

data class PaymentHeaderUiState(
    val tenantName: String,
    val groupName: String,
    val balance: UiText,
)
