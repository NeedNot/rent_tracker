package net.neednot.renttracker.feature.account.presentation.components

import androidx.compose.runtime.Composable
import net.neednot.renttracker.core.designsystem.components.ActionTone
import net.neednot.renttracker.core.designsystem.components.ConfirmDialog
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.account_delete_dialog_confirm
import net.neednot.renttracker.resources.account_delete_dialog_message
import net.neednot.renttracker.resources.account_delete_dialog_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun RequestDeletionDialog(
    onConfirmed: () -> Unit,
    onDismissed: () -> Unit,
) {
    ConfirmDialog(
        title = stringResource(Res.string.account_delete_dialog_title),
        text = stringResource(Res.string.account_delete_dialog_message),
        confirmLabel = stringResource(Res.string.account_delete_dialog_confirm),
        onConfirmed = onConfirmed,
        onDismissed = onDismissed,
        confirmTone = ActionTone.Destructive,
    )
}
