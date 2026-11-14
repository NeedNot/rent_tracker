package net.neednot.renttracker.feature.tenants.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

data class RentTermRowUiState(
    val rentTermId: String,
    val summary: UiText,
    val period: UiText,
    val isCurrent: Boolean,
)
