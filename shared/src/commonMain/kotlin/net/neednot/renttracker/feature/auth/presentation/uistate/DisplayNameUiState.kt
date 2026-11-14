package net.neednot.renttracker.feature.auth.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

data class DisplayNameUiState(
    val displayName: String,
    val error: UiText? = null,
    val isSaving: Boolean = false,
)
