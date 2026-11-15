package net.neednot.renttracker.feature.tenants.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

data class PendingMatchUiState(
    val parts: List<UiText>,
    val amount: String,
)
