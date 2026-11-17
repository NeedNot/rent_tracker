package net.neednot.renttracker.core.designsystem.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.common_cancel
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ConfirmDialog(
    title: String,
    confirmLabel: String,
    onConfirmed: () -> Unit,
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    dismissLabel: String = stringResource(Res.string.common_cancel),
    confirmTone: ActionTone = ActionTone.Default,
    isConfirmEnabled: Boolean = true,
) {
    RtDialog(
        title = title,
        onDismissed = onDismissed,
        confirmButton = {
            DialogButton(
                text = confirmLabel,
                onClick = onConfirmed,
                tone = confirmTone,
                enabled = isConfirmEnabled,
            )
        },
        modifier = modifier,
        dismissButton = { DialogButton(text = dismissLabel, onClick = onDismissed) },
        content = text?.let { { Text(it) } },
    )
}
