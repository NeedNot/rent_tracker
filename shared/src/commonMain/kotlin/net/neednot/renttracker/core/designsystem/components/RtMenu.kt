package net.neednot.renttracker.core.designsystem.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.designsystem.theme.Sizes
import net.neednot.renttracker.core.designsystem.theme.Spacing
import org.jetbrains.compose.resources.DrawableResource

private val MenuElevation = 3.dp

@Composable
internal fun RtMenu(
    expanded: Boolean,
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissed,
        modifier = modifier.width(Sizes.menu),
        shape = MaterialTheme.shapes.extraSmall,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
        shadowElevation = MenuElevation,
        content = content,
    )
}

@Composable
internal fun RtMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: DrawableResource? = null,
    tone: ActionTone = ActionTone.Default,
) {
    val scheme = MaterialTheme.colorScheme
    val color = if (tone == ActionTone.Destructive) scheme.error else scheme.onSurface
    DropdownMenuItem(
        text = { Text(text = text, style = MaterialTheme.typography.bodyLarge) },
        onClick = onClick,
        modifier = modifier,
        leadingIcon = icon?.let { { AppIcon(icon = it, contentDescription = null) } },
        colors = MenuDefaults.itemColors(textColor = color, leadingIconColor = color),
        contentPadding = PaddingValues(horizontal = Spacing.lg),
    )
}

@Composable
internal fun RtMenuDivider(modifier: Modifier = Modifier) {
    RtDivider(modifier = modifier.padding(vertical = Spacing.sm), color = RentTrackerTheme.colors.borderCard)
}
