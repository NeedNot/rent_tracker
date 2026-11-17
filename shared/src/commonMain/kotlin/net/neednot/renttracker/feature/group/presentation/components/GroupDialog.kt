package net.neednot.renttracker.feature.group.presentation.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import net.neednot.renttracker.core.designsystem.components.ActionTone
import net.neednot.renttracker.core.designsystem.components.ConfirmDialog
import net.neednot.renttracker.core.designsystem.components.DialogButton
import net.neednot.renttracker.core.designsystem.components.DialogCard
import net.neednot.renttracker.core.designsystem.components.RtDialog
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.group.presentation.uistate.GroupDialogUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.common_cancel
import net.neednot.renttracker.resources.group_archive_message
import net.neednot.renttracker.resources.group_archive_title
import net.neednot.renttracker.resources.group_leave_message
import net.neednot.renttracker.resources.group_leave_owner_message
import net.neednot.renttracker.resources.group_leave_owner_title
import net.neednot.renttracker.resources.group_leave_title
import net.neednot.renttracker.resources.group_menu_archive
import net.neednot.renttracker.resources.group_menu_leave
import net.neednot.renttracker.resources.group_transfer_ownership
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun GroupDialog(
    dialog: GroupDialogUiState,
    isWorking: Boolean,
    onConfirmed: () -> Unit,
    onDismissed: () -> Unit,
) {
    when (dialog) {
        is GroupDialogUiState.Archive -> {
            ArchiveDialog(dialog, isWorking, onConfirmed, onDismissed)
        }

        is GroupDialogUiState.Leave -> {
            ConfirmDialog(
                title = stringResource(Res.string.group_leave_title, dialog.groupName),
                text = stringResource(Res.string.group_leave_message),
                confirmLabel = stringResource(Res.string.group_menu_leave),
                onConfirmed = onConfirmed,
                onDismissed = onDismissed,
                confirmTone = ActionTone.Destructive,
                isConfirmEnabled = !isWorking,
            )
        }

        is GroupDialogUiState.LeaveOwner -> {
            ConfirmDialog(
                title = stringResource(Res.string.group_leave_owner_title),
                text = stringResource(Res.string.group_leave_owner_message, dialog.groupName),
                confirmLabel = stringResource(Res.string.group_transfer_ownership),
                onConfirmed = onConfirmed,
                onDismissed = onDismissed,
            )
        }
    }
}

@Composable
private fun ArchiveDialog(
    dialog: GroupDialogUiState.Archive,
    isWorking: Boolean,
    onConfirmed: () -> Unit,
    onDismissed: () -> Unit,
) {
    RtDialog(
        title = stringResource(Res.string.group_archive_title, dialog.groupName),
        onDismissed = onDismissed,
        confirmButton = {
            DialogButton(
                text = stringResource(Res.string.group_menu_archive),
                onClick = onConfirmed,
                tone = ActionTone.Destructive,
                enabled = !isWorking,
                isWorking = isWorking,
            )
        },
        dismissButton = { DialogButton(text = stringResource(Res.string.common_cancel), onClick = onDismissed) },
    ) {
        Text(stringResource(Res.string.group_archive_message))
        DialogCard(leadingText = dialog.tenantCount.asString(), trailingText = dialog.balance.asString())
    }
}
