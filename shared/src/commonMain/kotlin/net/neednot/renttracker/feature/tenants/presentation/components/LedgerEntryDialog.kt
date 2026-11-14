package net.neednot.renttracker.feature.tenants.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.designsystem.components.FormTextField
import net.neednot.renttracker.core.designsystem.components.MoneyField
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.tenants.presentation.uistate.CANCEL_REASON_MAX_LENGTH
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerDialogUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerSheetAction
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ledger_action_cancel_charge
import net.neednot.renttracker.resources.ledger_action_cancel_payment
import net.neednot.renttracker.resources.ledger_action_mark_bounced
import net.neednot.renttracker.resources.ledger_bounced_back
import net.neednot.renttracker.resources.ledger_bounced_title
import net.neednot.renttracker.resources.ledger_cancel_charge_title
import net.neednot.renttracker.resources.ledger_cancel_keep
import net.neednot.renttracker.resources.ledger_cancel_message
import net.neednot.renttracker.resources.ledger_cancel_payment_title
import net.neednot.renttracker.resources.ledger_cancel_reason_counter
import net.neednot.renttracker.resources.ledger_cancel_reason_label
import net.neednot.renttracker.resources.ledger_fee_add
import net.neednot.renttracker.resources.ledger_fee_amount_label
import net.neednot.renttracker.resources.ledger_fee_message
import net.neednot.renttracker.resources.ledger_fee_skip
import net.neednot.renttracker.resources.ledger_fee_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LedgerEntryDialog(
    dialog: LedgerDialogUiState,
    onTextChanged: (String) -> Unit,
    onConfirmed: () -> Unit,
    onDismissed: () -> Unit,
) {
    when (dialog) {
        is LedgerDialogUiState.Cancel -> {
            CancelDialog(dialog, onTextChanged, onConfirmed, onDismissed)
        }

        is LedgerDialogUiState.ConfirmBounced -> {
            ConfirmBouncedDialog(dialog, onConfirmed, onDismissed)
        }

        is LedgerDialogUiState.BouncedFee -> {
            BouncedFeeDialog(dialog, onTextChanged, onConfirmed, onDismissed)
        }
    }
}

@Composable
private fun CancelDialog(
    dialog: LedgerDialogUiState.Cancel,
    onReasonChanged: (String) -> Unit,
    onConfirmed: () -> Unit,
    onDismissed: () -> Unit,
) {
    val isPayment = dialog.action == LedgerSheetAction.CancelPayment
    val confirmLabel =
        if (isPayment) Res.string.ledger_action_cancel_payment else Res.string.ledger_action_cancel_charge
    val title = if (isPayment) Res.string.ledger_cancel_payment_title else Res.string.ledger_cancel_charge_title
    AlertDialog(
        onDismissRequest = onDismissed,
        confirmButton = {
            TextButton(onClick = onConfirmed, enabled = !dialog.isWorking) {
                Text(stringResource(confirmLabel))
            }
        },
        dismissButton = { TextButton(onClick = onDismissed) { Text(stringResource(Res.string.ledger_cancel_keep)) } },
        title = { Text(stringResource(title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(Res.string.ledger_cancel_message))
                FormTextField(
                    label = stringResource(Res.string.ledger_cancel_reason_label),
                    text = dialog.reason,
                    onTextChanged = onReasonChanged,
                    error = dialog.error,
                    hint =
                        stringResource(
                            Res.string.ledger_cancel_reason_counter,
                            dialog.reason.length,
                            CANCEL_REASON_MAX_LENGTH,
                        ),
                    enabled = !dialog.isWorking,
                )
            }
        },
    )
}

@Composable
private fun ConfirmBouncedDialog(
    dialog: LedgerDialogUiState.ConfirmBounced,
    onConfirmed: () -> Unit,
    onDismissed: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissed,
        confirmButton = {
            TextButton(onClick = onConfirmed, enabled = !dialog.isWorking) {
                Text(stringResource(Res.string.ledger_action_mark_bounced))
            }
        },
        dismissButton = { TextButton(onClick = onDismissed) { Text(stringResource(Res.string.ledger_bounced_back)) } },
        title = { Text(stringResource(Res.string.ledger_bounced_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(dialog.message.asString())
                dialog.error?.let { ErrorText(it) }
            }
        },
    )
}

@Composable
private fun BouncedFeeDialog(
    dialog: LedgerDialogUiState.BouncedFee,
    onAmountChanged: (String) -> Unit,
    onConfirmed: () -> Unit,
    onDismissed: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissed,
        confirmButton = {
            TextButton(onClick = onConfirmed, enabled = !dialog.isWorking) {
                Text(stringResource(Res.string.ledger_fee_add))
            }
        },
        dismissButton = { TextButton(onClick = onDismissed) { Text(stringResource(Res.string.ledger_fee_skip)) } },
        title = { Text(stringResource(Res.string.ledger_fee_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(Res.string.ledger_fee_message))
                MoneyField(
                    label = stringResource(Res.string.ledger_fee_amount_label),
                    text = dialog.amount,
                    onTextChanged = onAmountChanged,
                    error = dialog.error,
                    enabled = !dialog.isWorking,
                )
            }
        },
    )
}

@Composable
private fun ErrorText(text: UiText) {
    Text(
        text = text.asString(),
        modifier = Modifier.padding(top = 4.dp),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
    )
}
