package net.neednot.renttracker.feature.tenants.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerItemUiState

@Composable
internal fun LedgerItem(
    item: LedgerItemUiState,
    actions: LedgerItemActions,
    modifier: Modifier = Modifier,
) {
    when (item) {
        is LedgerItemUiState.Entry -> {
            LedgerRow(
                row = item.row,
                onClicked = { actions.onEntryClicked(item.row.key) },
                onRestoreClicked = { actions.onRestoreClicked(item.row.key) },
                modifier = modifier,
            )
        }

        is LedgerItemUiState.Pending -> {
            PendingLedgerRow(
                row = item.row,
                onClicked = { actions.onPendingClicked(item.row.clientId) },
                modifier = modifier,
            )
        }
    }
}
