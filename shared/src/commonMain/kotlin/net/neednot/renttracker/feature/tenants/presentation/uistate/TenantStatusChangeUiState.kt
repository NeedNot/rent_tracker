package net.neednot.renttracker.feature.tenants.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

data class TenantStatusChangeUiState(
    val confirmation: TenantConfirmationUiState? = null,
    val isSaving: Boolean = false,
    val error: UiText? = null,
)
