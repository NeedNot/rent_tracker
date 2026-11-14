package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerDialogUiState

internal inline fun <reified D : LedgerDialogUiState> MutableStateFlow<LedgerDialogUiState?>.updateAs(
    crossinline transform: (D) -> D,
) {
    update { current -> if (current is D) transform(current) else current }
}
