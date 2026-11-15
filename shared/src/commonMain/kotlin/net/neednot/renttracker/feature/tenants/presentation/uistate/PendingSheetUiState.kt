package net.neednot.renttracker.feature.tenants.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

data class PendingSheetUiState(
    val clientId: String,
    val title: UiText,
    val chip: PendingChip,
    val amount: String,
    val body: PendingSheetBody,
    val actions: List<PendingSheetAction>,
)
