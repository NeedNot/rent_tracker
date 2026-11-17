package net.neednot.renttracker.feature.tenants.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import net.neednot.renttracker.core.designsystem.components.ActionTone
import net.neednot.renttracker.core.designsystem.components.ConfirmDialog
import net.neednot.renttracker.core.designsystem.components.RtBottomSheet
import net.neednot.renttracker.core.navigation.TenantRoute
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingSheetAction
import net.neednot.renttracker.feature.tenants.presentation.viewmodel.PendingPaymentViewModel
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ledger_action_cancel_payment
import net.neednot.renttracker.resources.ledger_cancel_keep_payment
import net.neednot.renttracker.resources.pending_cancel_message
import net.neednot.renttracker.resources.pending_cancel_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

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
        RtBottomSheet(onDismissed = viewModel::onSheetDismissed) {
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
        ConfirmDialog(
            title = stringResource(Res.string.pending_cancel_title),
            text = stringResource(Res.string.pending_cancel_message),
            confirmLabel = stringResource(Res.string.ledger_action_cancel_payment),
            onConfirmed = viewModel::onCancelConfirmed,
            onDismissed = viewModel::onCancelDialogDismissed,
            dismissLabel = stringResource(Res.string.ledger_cancel_keep_payment),
            confirmTone = ActionTone.Destructive,
            isConfirmEnabled = !uiState.isWorking,
        )
    }
}
