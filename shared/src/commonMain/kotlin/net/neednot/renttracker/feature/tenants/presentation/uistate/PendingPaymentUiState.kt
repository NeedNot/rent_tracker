package net.neednot.renttracker.feature.tenants.presentation.uistate

data class PendingPaymentUiState(
    val sheet: PendingSheetUiState? = null,
    val isCancelDialogOpen: Boolean = false,
    val isWorking: Boolean = false,
)
