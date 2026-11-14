package net.neednot.renttracker.feature.tenants.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

sealed interface LedgerUiState {
    data object Loading : LedgerUiState

    data class Content(
        val tenantName: String,
        val groupName: String,
        val sections: List<LedgerSectionUiState>,
        val emptyMessage: UiText?,
        val canLogPayment: Boolean,
        val syncError: UiText? = null,
    ) : LedgerUiState

    data class Error(
        val message: UiText,
    ) : LedgerUiState
}
