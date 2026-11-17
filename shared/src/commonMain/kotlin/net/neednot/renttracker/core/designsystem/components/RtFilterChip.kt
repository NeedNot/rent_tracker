package net.neednot.renttracker.core.designsystem.components

import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import net.neednot.renttracker.core.designsystem.theme.Opacity
import net.neednot.renttracker.core.designsystem.theme.Sizes
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ic_check

@Composable
internal fun RtFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = text, style = MaterialTheme.typography.labelLarge) },
        modifier = modifier.alpha(if (enabled) 1f else Opacity.DISABLED),
        enabled = enabled,
        leadingIcon =
            if (selected) {
                { AppIcon(icon = Res.drawable.ic_check, contentDescription = null, size = IconSize.Small) }
            } else {
                null
            },
        shape = MaterialTheme.shapes.small,
        colors =
            FilterChipDefaults.filterChipColors(
                labelColor = scheme.onSurfaceVariant,
                selectedContainerColor = scheme.secondaryContainer,
                selectedLabelColor = scheme.onSecondaryContainer,
                selectedLeadingIconColor = scheme.onSecondaryContainer,
                disabledLabelColor = scheme.onSurfaceVariant,
                disabledSelectedContainerColor = scheme.secondaryContainer,
            ),
        border =
            FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = selected,
                borderColor = scheme.outline,
                borderWidth = Sizes.strokeThin,
            ),
    )
}
