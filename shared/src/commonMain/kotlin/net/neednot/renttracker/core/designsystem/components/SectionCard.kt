package net.neednot.renttracker.core.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import net.neednot.renttracker.core.designsystem.theme.Sizes
import net.neednot.renttracker.core.designsystem.theme.Spacing
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ic_chevron_right

@Composable
internal fun SectionCard(
    heading: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClicked: () -> Unit = {},
    footerLabel: String? = null,
    onFooterClicked: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    RtCard(modifier = modifier) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = Sizes.touchTarget)
                    .padding(start = Spacing.lg, end = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = heading,
                modifier = Modifier.weight(1f).semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
            )
            actionLabel?.let { RtButton(text = it, onClick = onActionClicked, variant = ButtonVariant.Text) }
        }
        RtDivider()
        content()
        footerLabel?.let { SectionCardFooter(label = it, onClick = onFooterClicked) }
    }
}

@Composable
private fun SectionCardFooter(
    label: String,
    onClick: () -> Unit,
) {
    RtDivider()
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .heightIn(min = Sizes.touchTarget),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        AppIcon(
            icon = Res.drawable.ic_chevron_right,
            contentDescription = null,
            size = IconSize.Small,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}
