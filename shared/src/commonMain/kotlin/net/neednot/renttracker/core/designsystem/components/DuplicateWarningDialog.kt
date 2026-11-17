package net.neednot.renttracker.core.designsystem.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun DuplicateWarningDialog(
    title: String,
    body: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirmed: () -> Unit,
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier,
    lines: List<DialogCardLine> = emptyList(),
) {
    RtDialog(
        title = title,
        onDismissed = onDismissed,
        modifier = modifier,
        confirmButton = { DialogButton(text = confirmLabel, onClick = onConfirmed) },
        dismissButton = { DialogButton(text = dismissLabel, onClick = onDismissed) },
    ) {
        Text(body)
        lines.forEach { line -> DialogCard(leadingText = line.leadingText, trailingText = line.trailingText) }
    }
}
