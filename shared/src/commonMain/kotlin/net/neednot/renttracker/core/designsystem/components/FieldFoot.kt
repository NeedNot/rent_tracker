package net.neednot.renttracker.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.neednot.renttracker.core.designsystem.theme.Spacing
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ic_info

@Composable
internal fun FieldFoot(
    modifier: Modifier = Modifier,
    error: String? = null,
    hint: String? = null,
    counter: String? = null,
) {
    if (error == null && hint == null && counter == null) return
    val scheme = MaterialTheme.colorScheme
    val style = MaterialTheme.typography.bodySmall
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            when {
                error != null -> {
                    AppIcon(
                        icon = Res.drawable.ic_info,
                        contentDescription = null,
                        size = IconSize.Small,
                        tint = scheme.error,
                    )
                    Text(text = error, color = scheme.error, style = style)
                }

                hint != null -> {
                    Text(text = hint, color = scheme.onSurfaceVariant, style = style)
                }
            }
        }
        counter?.let { Text(text = it, color = scheme.onSurfaceVariant, style = style) }
    }
}
