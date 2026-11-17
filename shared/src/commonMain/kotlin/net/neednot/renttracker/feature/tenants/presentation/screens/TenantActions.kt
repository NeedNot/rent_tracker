package net.neednot.renttracker.feature.tenants.presentation.screens

import net.neednot.renttracker.feature.tenants.presentation.uistate.LateFeeSuggestionUiState

internal data class TenantActions(
    val onBackClicked: () -> Unit = {},
    val onEditDetailsClicked: (groupId: String) -> Unit = {},
    val onChangeRentClicked: () -> Unit = {},
    val onRentTermClicked: (rentTermId: String) -> Unit = {},
    val onLogPaymentClicked: () -> Unit = {},
    val onAddChargeClicked: () -> Unit = {},
    val onViewLedgerClicked: () -> Unit = {},
    val onLateFeeAddClicked: (LateFeeSuggestionUiState) -> Unit = {},
    val onLateFeeDismissed: () -> Unit = {},
    val onMoveClicked: () -> Unit = {},
    val onArchiveClicked: () -> Unit = {},
    val onRestoreClicked: () -> Unit = {},
    val onStatusChangeConfirmed: () -> Unit = {},
    val onStatusChangeDismissed: () -> Unit = {},
)
