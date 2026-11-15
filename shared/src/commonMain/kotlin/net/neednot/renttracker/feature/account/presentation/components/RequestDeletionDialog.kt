package net.neednot.renttracker.feature.account.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.account_delete_dialog_confirm
import net.neednot.renttracker.resources.account_delete_dialog_message
import net.neednot.renttracker.resources.account_delete_dialog_title
import net.neednot.renttracker.resources.common_cancel
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun RequestDeletionDialog(
    onConfirmed: () -> Unit,
    onDismissed: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissed,
        title = { Text(stringResource(Res.string.account_delete_dialog_title)) },
        text = { Text(stringResource(Res.string.account_delete_dialog_message)) },
        confirmButton = {
            TextButton(onClick = onConfirmed) {
                Text(
                    text = stringResource(Res.string.account_delete_dialog_confirm),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismissed) { Text(stringResource(Res.string.common_cancel)) } },
    )
}
