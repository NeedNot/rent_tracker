package net.neednot.renttracker.feature.group.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import net.neednot.renttracker.core.designsystem.theme.Spacing

@Composable
internal fun GroupSummary(
    balance: String,
    tenantCounts: String,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth().padding(Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            text = balance,
            modifier = Modifier.weight(1f).alignByBaseline(),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = tenantCounts,
            modifier = Modifier.alignByBaseline(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
