package net.neednot.renttracker.feature.tenants.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

data class PendingRowUiState(
    val clientId: String,
    val date: String,
    val title: UiText,
    val chip: PendingChip,
    val detail: UiText,
    val amount: UiText,
)
