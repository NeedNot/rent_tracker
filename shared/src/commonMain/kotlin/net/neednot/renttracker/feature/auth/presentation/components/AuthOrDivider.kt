package net.neednot.renttracker.feature.auth.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import net.neednot.renttracker.core.designsystem.components.RtDivider
import net.neednot.renttracker.core.designsystem.theme.Spacing
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.auth_or_divider
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AuthOrDivider(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RtDivider(modifier = Modifier.weight(1f), color = scheme.outlineVariant)
        Text(
            text = stringResource(Res.string.auth_or_divider),
            color = scheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        RtDivider(modifier = Modifier.weight(1f), color = scheme.outlineVariant)
    }
}
