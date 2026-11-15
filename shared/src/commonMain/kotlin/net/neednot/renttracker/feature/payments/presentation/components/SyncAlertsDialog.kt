package net.neednot.renttracker.feature.payments.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.payment_sync_alerts_allow
import net.neednot.renttracker.resources.payment_sync_alerts_message
import net.neednot.renttracker.resources.payment_sync_alerts_not_now
import net.neednot.renttracker.resources.payment_sync_alerts_permission_hint
import net.neednot.renttracker.resources.payment_sync_alerts_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SyncAlertsDialog(
    onAllowed: () -> Unit,
    onDeclined: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDeclined,
        title = { Text(stringResource(Res.string.payment_sync_alerts_title)) },
        text = {
            Column {
                Text(stringResource(Res.string.payment_sync_alerts_message))
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.payment_sync_alerts_permission_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onAllowed,
            ) { Text(stringResource(Res.string.payment_sync_alerts_allow)) }
        },
        dismissButton = {
            TextButton(onClick = onDeclined) { Text(stringResource(Res.string.payment_sync_alerts_not_now)) }
        },
    )
}
