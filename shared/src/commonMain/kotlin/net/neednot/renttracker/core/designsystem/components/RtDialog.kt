package net.neednot.renttracker.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.designsystem.theme.Spacing

@Composable
internal fun RtDialog(
    title: String,
    onDismissed: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismissed,
        confirmButton = confirmButton,
        modifier = modifier,
        dismissButton = dismissButton,
        title = { Text(title) },
        text = content?.let { { Column(verticalArrangement = Arrangement.spacedBy(Spacing.md), content = it) } },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
    )
}

@Composable
internal fun DialogButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: ActionTone = ActionTone.Default,
    enabled: Boolean = true,
    isWorking: Boolean = false,
) {
    RtButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        variant = ButtonVariant.Text,
        tone = tone,
        enabled = enabled,
        isWorking = isWorking,
    )
}
