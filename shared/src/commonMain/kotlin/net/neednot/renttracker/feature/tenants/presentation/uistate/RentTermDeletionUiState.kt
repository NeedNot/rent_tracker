package net.neednot.renttracker.feature.tenants.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

data class RentTermDeletionUiState(
    val reason: String = "",
    val error: UiText? = null,
    val isDeleting: Boolean = false,
)
