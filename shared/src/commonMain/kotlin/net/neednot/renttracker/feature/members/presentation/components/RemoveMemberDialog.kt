package net.neednot.renttracker.feature.members.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import net.neednot.renttracker.core.designsystem.components.ActionTone
import net.neednot.renttracker.core.designsystem.components.DialogButton
import net.neednot.renttracker.core.designsystem.components.RtDialog
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.members.presentation.uistate.RemoveMemberDialogUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.common_cancel
import net.neednot.renttracker.resources.members_remove
import net.neednot.renttracker.resources.members_remove_message
import net.neednot.renttracker.resources.members_remove_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun RemoveMemberDialog(
    dialog: RemoveMemberDialogUiState,
    onConfirmed: () -> Unit,
    onDismissed: () -> Unit,
) {
    RtDialog(
        title = stringResource(Res.string.members_remove_title, dialog.memberName),
        onDismissed = onDismissed,
        confirmButton = {
            DialogButton(
                text = stringResource(Res.string.members_remove),
                onClick = onConfirmed,
                tone = ActionTone.Destructive,
                enabled = !dialog.isWorking,
                isWorking = dialog.isWorking,
            )
        },
        dismissButton = {
            DialogButton(
                text = stringResource(Res.string.common_cancel),
                onClick = onDismissed,
                enabled = !dialog.isWorking,
            )
        },
    ) {
        Text(stringResource(Res.string.members_remove_message, dialog.memberFirstName))
        dialog.error?.let { Text(text = it.asString(), color = MaterialTheme.colorScheme.error) }
    }
}
