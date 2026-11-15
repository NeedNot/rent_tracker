package net.neednot.renttracker.feature.members.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.feature.members.presentation.uistate.TransferConfirmUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.common_cancel
import net.neednot.renttracker.resources.members_transfer_confirm
import net.neednot.renttracker.resources.members_transfer_confirm_message
import net.neednot.renttracker.resources.members_transfer_confirm_title
import net.neednot.renttracker.resources.members_transfer_confirm_you
import net.neednot.renttracker.resources.members_transfer_working
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TransferConfirmDialog(
    confirm: TransferConfirmUiState,
    onConfirmed: () -> Unit,
    onDismissed: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissed,
        confirmButton = {
            TextButton(onClick = onConfirmed, enabled = !confirm.isWorking) {
                val label =
                    if (confirm.isWorking) Res.string.members_transfer_working else Res.string.members_transfer_confirm
                Text(stringResource(label))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissed,
                enabled = !confirm.isWorking,
            ) { Text(stringResource(Res.string.common_cancel)) }
        },
        title = { Text(stringResource(Res.string.members_transfer_confirm_title, confirm.newOwnerName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(
                        Res.string.members_transfer_confirm_message,
                        confirm.newOwnerFirstName,
                        confirm.groupName,
                    ),
                )
                Text(stringResource(Res.string.members_transfer_confirm_you, confirm.newOwnerFirstName))
            }
        },
    )
}
