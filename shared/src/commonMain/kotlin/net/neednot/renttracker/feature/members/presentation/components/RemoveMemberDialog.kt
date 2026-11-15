package net.neednot.renttracker.feature.members.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.members.presentation.uistate.RemoveMemberDialogUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.members_remove_confirm
import net.neednot.renttracker.resources.members_remove_entries
import net.neednot.renttracker.resources.members_remove_keep
import net.neednot.renttracker.resources.members_remove_message
import net.neednot.renttracker.resources.members_remove_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun RemoveMemberDialog(
    dialog: RemoveMemberDialogUiState,
    onConfirmed: () -> Unit,
    onDismissed: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissed,
        confirmButton = {
            TextButton(onClick = onConfirmed, enabled = !dialog.isWorking) {
                Text(stringResource(Res.string.members_remove_confirm), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissed, enabled = !dialog.isWorking) {
                Text(stringResource(Res.string.members_remove_keep))
            }
        },
        title = { Text(stringResource(Res.string.members_remove_title, dialog.memberName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.members_remove_message, dialog.memberFirstName, dialog.groupName))
                Text(stringResource(Res.string.members_remove_entries))
                dialog.error?.let { error -> Text(text = error.asString(), color = MaterialTheme.colorScheme.error) }
            }
        },
    )
}
