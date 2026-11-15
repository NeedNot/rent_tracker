package net.neednot.renttracker.feature.tenants.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

data class MoveConfirmationUiState(
    val title: UiText,
    val consequences: List<UiText>,
)
