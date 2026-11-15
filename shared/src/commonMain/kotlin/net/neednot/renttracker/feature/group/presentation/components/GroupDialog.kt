package net.neednot.renttracker.feature.group.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.group.presentation.uistate.GroupDialogUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.common_cancel
import net.neednot.renttracker.resources.group_archive_confirm
import net.neednot.renttracker.resources.group_archive_message
import net.neednot.renttracker.resources.group_archive_title
import net.neednot.renttracker.resources.group_leave_confirm
import net.neednot.renttracker.resources.group_leave_message
import net.neednot.renttracker.resources.group_leave_owner_message
import net.neednot.renttracker.resources.group_leave_owner_title
import net.neednot.renttracker.resources.group_leave_owner_transfer
import net.neednot.renttracker.resources.group_leave_stay
import net.neednot.renttracker.resources.group_leave_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun GroupDialog(
    dialog: GroupDialogUiState,
    isWorking: Boolean,
    onConfirmed: () -> Unit,
    onDismissed: () -> Unit,
) {
    when (dialog) {
        is GroupDialogUiState.Archive -> ArchiveDialog(dialog, isWorking, onConfirmed, onDismissed)
        is GroupDialogUiState.Leave -> LeaveDialog(dialog, isWorking, onConfirmed, onDismissed)
        is GroupDialogUiState.LeaveOwner -> LeaveOwnerDialog(dialog, onConfirmed, onDismissed)
    }
}

@Composable
private fun ArchiveDialog(
    dialog: GroupDialogUiState.Archive,
    isWorking: Boolean,
    onConfirmed: () -> Unit,
    onDismissed: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissed,
        confirmButton = {
            TextButton(onClick = onConfirmed, enabled = !isWorking) {
                Text(stringResource(Res.string.group_archive_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismissed) { Text(stringResource(Res.string.common_cancel)) } },
        title = { Text(stringResource(Res.string.group_archive_title, dialog.groupName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(dialog.tenantCount.asString(), style = MaterialTheme.typography.titleMedium)
                    Text(dialog.balance.asString(), style = MaterialTheme.typography.titleMedium)
                }
                Text(stringResource(Res.string.group_archive_message))
            }
        },
    )
}

@Composable
private fun LeaveDialog(
    dialog: GroupDialogUiState.Leave,
    isWorking: Boolean,
    onConfirmed: () -> Unit,
    onDismissed: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissed,
        confirmButton = {
            TextButton(onClick = onConfirmed, enabled = !isWorking) {
                Text(text = stringResource(Res.string.group_leave_confirm), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismissed) { Text(stringResource(Res.string.group_leave_stay)) } },
        title = { Text(stringResource(Res.string.group_leave_title, dialog.groupName)) },
        text = { Text(stringResource(Res.string.group_leave_message)) },
    )
}

@Composable
private fun LeaveOwnerDialog(
    dialog: GroupDialogUiState.LeaveOwner,
    onTransferClicked: () -> Unit,
    onDismissed: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissed,
        confirmButton = {
            TextButton(onClick = onTransferClicked) { Text(stringResource(Res.string.group_leave_owner_transfer)) }
        },
        dismissButton = { TextButton(onClick = onDismissed) { Text(stringResource(Res.string.common_cancel)) } },
        title = { Text(stringResource(Res.string.group_leave_owner_title)) },
        text = { Text(stringResource(Res.string.group_leave_owner_message, dialog.groupName)) },
    )
}
