package net.neednot.renttracker.feature.tenants.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import net.neednot.renttracker.core.navigation.TenantRoute
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingSheetAction
import net.neednot.renttracker.feature.tenants.presentation.viewmodel.PendingPaymentViewModel
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ledger_action_cancel_payment
import net.neednot.renttracker.resources.ledger_cancel_keep
import net.neednot.renttracker.resources.pending_cancel_message
import net.neednot.renttracker.resources.pending_cancel_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PendingPaymentHost(
    route: TenantRoute,
    onEditClicked: (clientId: String) -> Unit,
    viewModel: PendingPaymentViewModel = koinViewModel { parametersOf(route) },
    content: @Composable (onPendingClicked: (clientId: String) -> Unit) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    content(viewModel::onPendingPaymentClicked)
    uiState.sheet?.let { sheet ->
        ModalBottomSheet(onDismissRequest = viewModel::onSheetDismissed) {
            PendingPaymentSheet(
                sheet = sheet,
                isWorking = uiState.isWorking,
                onActionClicked = { action ->
                    viewModel.onActionClicked(action)
                    if (action == PendingSheetAction.Edit) onEditClicked(sheet.clientId)
                },
            )
        }
    }
    if (uiState.isCancelDialogOpen) {
        AlertDialog(
            onDismissRequest = viewModel::onCancelDialogDismissed,
            confirmButton = {
                TextButton(onClick = viewModel::onCancelConfirmed, enabled = !uiState.isWorking) {
                    Text(stringResource(Res.string.ledger_action_cancel_payment))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onCancelDialogDismissed) {
                    Text(stringResource(Res.string.ledger_cancel_keep))
                }
            },
            title = { Text(stringResource(Res.string.pending_cancel_title)) },
            text = { Text(stringResource(Res.string.pending_cancel_message)) },
        )
    }
}
