package net.neednot.renttracker.feature.members.presentation.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import net.neednot.renttracker.core.designsystem.components.DialogButton
import net.neednot.renttracker.core.designsystem.components.RtDialog
import net.neednot.renttracker.feature.members.presentation.uistate.TransferConfirmUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.common_cancel
import net.neednot.renttracker.resources.members_transfer_confirm
import net.neednot.renttracker.resources.members_transfer_confirm_message
import net.neednot.renttracker.resources.members_transfer_confirm_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TransferConfirmDialog(
    confirm: TransferConfirmUiState,
    onConfirmed: () -> Unit,
    onDismissed: () -> Unit,
) {
    RtDialog(
        title = stringResource(Res.string.members_transfer_confirm_title, confirm.newOwnerName),
        onDismissed = onDismissed,
        confirmButton = {
            DialogButton(
                text = stringResource(Res.string.members_transfer_confirm),
                onClick = onConfirmed,
                enabled = !confirm.isWorking,
                isWorking = confirm.isWorking,
            )
        },
        dismissButton = {
            DialogButton(
                text = stringResource(Res.string.common_cancel),
                onClick = onDismissed,
                enabled = !confirm.isWorking,
            )
        },
    ) {
        Text(
            stringResource(
                Res.string.members_transfer_confirm_message,
                confirm.newOwnerFirstName,
                confirm.groupName,
            ),
        )
    }
}
