package net.neednot.renttracker.feature.account.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

data class EditNameDialogUiState(
    val name: String,
    val error: UiText? = null,
    val isSaving: Boolean = false,
)
