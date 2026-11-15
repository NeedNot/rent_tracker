package net.neednot.renttracker.feature.home.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.home_late_rent_alerts_allow
import net.neednot.renttracker.resources.home_late_rent_alerts_message
import net.neednot.renttracker.resources.home_late_rent_alerts_not_now
import net.neednot.renttracker.resources.home_late_rent_alerts_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LateRentAlertsDialog(
    onAllowed: () -> Unit,
    onDeclined: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDeclined,
        title = { Text(stringResource(Res.string.home_late_rent_alerts_title)) },
        text = { Text(stringResource(Res.string.home_late_rent_alerts_message)) },
        confirmButton = {
            TextButton(onClick = onAllowed) { Text(stringResource(Res.string.home_late_rent_alerts_allow)) }
        },
        dismissButton = {
            TextButton(onClick = onDeclined) { Text(stringResource(Res.string.home_late_rent_alerts_not_now)) }
        },
    )
}
