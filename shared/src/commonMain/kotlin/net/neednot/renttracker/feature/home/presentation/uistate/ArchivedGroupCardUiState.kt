package net.neednot.renttracker.feature.home.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

data class ArchivedGroupCardUiState(
    val groupId: String,
    val name: String,
    val balance: UiText,
    val summary: UiText,
)
