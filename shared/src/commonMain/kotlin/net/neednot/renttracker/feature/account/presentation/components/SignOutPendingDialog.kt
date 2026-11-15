package net.neednot.renttracker.feature.account.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.account_sign_out_pending_confirm
import net.neednot.renttracker.resources.account_sign_out_pending_message
import net.neednot.renttracker.resources.account_sign_out_pending_stay
import net.neednot.renttracker.resources.account_sign_out_pending_title
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SignOutPendingDialog(
    unsyncedCount: Int,
    onConfirmed: () -> Unit,
    onDismissed: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissed,
        title = { Text(stringResource(Res.string.account_sign_out_pending_title)) },
        text = {
            Text(pluralStringResource(Res.plurals.account_sign_out_pending_message, unsyncedCount, unsyncedCount))
        },
        confirmButton = {
            TextButton(onClick = onConfirmed) {
                Text(
                    text = stringResource(Res.string.account_sign_out_pending_confirm),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissed) { Text(stringResource(Res.string.account_sign_out_pending_stay)) }
        },
    )
}
