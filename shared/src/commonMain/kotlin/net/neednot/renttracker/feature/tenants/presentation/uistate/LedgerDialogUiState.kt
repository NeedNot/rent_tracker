package net.neednot.renttracker.feature.tenants.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

const val CANCEL_REASON_MAX_LENGTH = 100

sealed interface LedgerDialogUiState {
    val error: UiText?
    val isWorking: Boolean

    data class Cancel(
        val action: LedgerSheetAction,
        val reason: String = "",
        override val error: UiText? = null,
        override val isWorking: Boolean = false,
    ) : LedgerDialogUiState

    data class ConfirmBounced(
        val message: UiText,
        override val error: UiText? = null,
        override val isWorking: Boolean = false,
    ) : LedgerDialogUiState

    data class BouncedFee(
        val amount: String = "",
        override val error: UiText? = null,
        override val isWorking: Boolean = false,
    ) : LedgerDialogUiState
}
