package net.neednot.renttracker.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import net.neednot.renttracker.core.designsystem.theme.ContainerColors
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.designsystem.theme.Sizes
import net.neednot.renttracker.core.designsystem.theme.Spacing
import org.jetbrains.compose.resources.DrawableResource

@Composable
internal fun Tag(
    text: String,
    kind: TagKind,
    modifier: Modifier = Modifier,
    icon: DrawableResource? = null,
) {
    val colors = kind.colors()
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = colors.container,
        contentColor = colors.onContainer,
        border =
            if (kind == TagKind.Outlined) BorderStroke(Sizes.strokeThin, MaterialTheme.colorScheme.outline) else null,
    ) {
        Row(
            modifier = Modifier.heightIn(min = Sizes.tag).padding(horizontal = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            icon?.let { AppIcon(icon = it, contentDescription = null, size = IconSize.Small) }
            Text(text = text, maxLines = 1, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun TagKind.colors(): ContainerColors {
    val scheme = MaterialTheme.colorScheme
    return when (this) {
        TagKind.Neutral -> {
            RentTrackerTheme.colors.statusPending
        }

        TagKind.Outlined -> {
            ContainerColors(container = Color.Transparent, onContainer = scheme.onSurfaceVariant)
        }

        TagKind.Error -> {
            RentTrackerTheme.colors.statusLate
        }

        TagKind.Warning -> {
            RentTrackerTheme.colors.statusOwes
        }

        TagKind.Current -> {
            ContainerColors(container = scheme.secondaryContainer, onContainer = scheme.onSecondaryContainer)
        }
    }
}
