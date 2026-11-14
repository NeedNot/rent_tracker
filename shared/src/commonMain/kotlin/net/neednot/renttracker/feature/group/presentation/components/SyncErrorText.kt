package net.neednot.renttracker.feature.group.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.asString

@Composable
fun SyncErrorText(
    error: UiText,
    modifier: Modifier = Modifier,
) {
    Text(
        text = error.asString(),
        modifier = modifier,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
    )
}
