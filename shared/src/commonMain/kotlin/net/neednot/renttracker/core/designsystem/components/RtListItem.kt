package net.neednot.renttracker.core.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import net.neednot.renttracker.core.designsystem.theme.Opacity
import net.neednot.renttracker.core.designsystem.theme.Sizes
import net.neednot.renttracker.core.designsystem.theme.Spacing
import org.jetbrains.compose.resources.DrawableResource

@Composable
internal fun RtListItem(
    text: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    leadingIcon: DrawableResource? = null,
    tone: ActionTone = ActionTone.Default,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
                .alpha(if (enabled) 1f else Opacity.DISABLED)
                .heightIn(min = Sizes.field)
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leadingIcon?.let { AppIcon(icon = it, contentDescription = null, tint = scheme.onSurfaceVariant) }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                text = text,
                color = if (tone == ActionTone.Destructive) scheme.error else scheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
            )
            supportingText?.let {
                Text(text = it, color = scheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
        }
        trailing?.invoke()
    }
}
