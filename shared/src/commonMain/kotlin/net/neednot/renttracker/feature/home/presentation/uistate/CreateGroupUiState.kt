package net.neednot.renttracker.feature.home.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

data class CreateGroupUiState(
    val name: String = "",
    val error: UiText? = null,
    val isSaving: Boolean = false,
)
