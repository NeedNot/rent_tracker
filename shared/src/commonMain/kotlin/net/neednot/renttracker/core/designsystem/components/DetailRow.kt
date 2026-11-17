package net.neednot.renttracker.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import net.neednot.renttracker.core.designsystem.theme.Spacing

@Composable
internal fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    layout: DetailLayout = DetailLayout.Stacked,
) {
    val scheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    Column(modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        RtDivider()
        when (layout) {
            DetailLayout.Stacked -> {
                Column(
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Text(text = label, color = scheme.onSurfaceVariant, style = typography.bodySmall)
                    Text(text = value, style = typography.bodyMedium)
                }
            }

            DetailLayout.Inline -> {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
                ) {
                    Text(text = label, color = scheme.onSurfaceVariant, style = typography.bodyMedium)
                    Text(
                        text = value,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End,
                        style = typography.bodyMedium,
                    )
                }
            }
        }
    }
}
