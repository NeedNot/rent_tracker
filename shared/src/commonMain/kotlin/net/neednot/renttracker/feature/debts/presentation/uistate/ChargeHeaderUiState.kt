package net.neednot.renttracker.feature.debts.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

data class ChargeHeaderUiState(
    val tenantName: String,
    val groupName: String,
    val balance: UiText,
)
