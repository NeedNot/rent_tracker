package net.neednot.renttracker.feature.tenants.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.designsystem.theme.Sizes
import net.neednot.renttracker.core.designsystem.theme.Spacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LedgerRow(
    date: String,
    title: String,
    amount: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    style: LedgerRowStyle = LedgerRowStyle.Normal,
    isPaidAmount: Boolean = false,
    balance: String? = null,
    meta: (@Composable FlowRowScope.() -> Unit)? = null,
    endAction: (@Composable () -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val inactive = RentTrackerTheme.colors.entryInactive
    val isInactive = style != LedgerRowStyle.Normal
    val strongColor = if (isInactive) inactive.onContainer else scheme.onSurface
    val dateColor = if (isInactive) inactive.onContainer else scheme.onSurfaceVariant
    val amountColor =
        when {
            isInactive -> inactive.onContainer
            isPaidAmount -> RentTrackerTheme.colors.statusPaid.onContainer
            else -> scheme.onSurface
        }
    val decoration = if (style == LedgerRowStyle.Void) TextDecoration.LineThrough else TextDecoration.None
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(if (isInactive) inactive.container else Color.Transparent)
                .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
                .heightIn(min = Sizes.touchTarget)
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            text = date,
            modifier = Modifier.width(Sizes.touchTarget),
            color = dateColor,
            style = MaterialTheme.typography.bodySmall,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                text = title,
                color = strongColor,
                textDecoration = decoration,
                style = MaterialTheme.typography.titleSmall,
            )
            meta?.let {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    itemVerticalAlignment = Alignment.CenterVertically,
                    content = it,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                text = amount,
                color = amountColor,
                textAlign = TextAlign.End,
                textDecoration = decoration,
                maxLines = 1,
                style = MaterialTheme.typography.titleSmall,
            )
            balance?.let {
                Text(
                    text = it,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            endAction?.invoke()
        }
    }
}

@Composable
internal fun LedgerMeta(text: String) {
    Text(text = text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
}
