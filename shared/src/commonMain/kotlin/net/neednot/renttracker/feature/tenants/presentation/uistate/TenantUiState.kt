package net.neednot.renttracker.feature.tenants.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

sealed interface TenantUiState {
    data object Loading : TenantUiState

    data class Content(
        val name: String,
        val groupId: String,
        val groupName: String,
        val isArchived: Boolean,
        val balance: TenantBalanceUiState?,
        val rentTerms: List<RentTermRowUiState>,
        val details: TenantDetailsUiState,
        val ledgerPreview: List<LedgerRowUiState> = emptyList(),
        val canLogPayment: Boolean = false,
        val canAddCharge: Boolean = false,
        val lateFeeSuggestion: LateFeeSuggestionUiState? = null,
        val syncError: UiText? = null,
        val statusChange: TenantStatusChangeUiState = TenantStatusChangeUiState(),
    ) : TenantUiState

    data class Error(
        val message: UiText,
    ) : TenantUiState
}
