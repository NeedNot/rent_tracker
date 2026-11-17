package net.neednot.renttracker.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import net.neednot.renttracker.core.designsystem.theme.Sizes
import net.neednot.renttracker.core.designsystem.theme.Spacing
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ic_chevron_right
import net.neednot.renttracker.resources.ic_inventory_2

@Composable
internal fun ArchivedRow(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier.heightIn(min = Sizes.touchTarget).padding(start = Spacing.lg, end = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(icon = Res.drawable.ic_inventory_2, contentDescription = null)
            Text(text = text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
            AppIcon(icon = Res.drawable.ic_chevron_right, contentDescription = null)
        }
    }
}
