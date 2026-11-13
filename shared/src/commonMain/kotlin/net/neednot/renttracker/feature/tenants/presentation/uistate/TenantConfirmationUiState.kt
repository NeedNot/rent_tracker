package net.neednot.renttracker.feature.tenants.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

data class TenantConfirmationUiState(
    val action: TenantStatusAction,
    val message: UiText,
)
