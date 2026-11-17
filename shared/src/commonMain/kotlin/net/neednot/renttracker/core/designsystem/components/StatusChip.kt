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
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ic_check_circle_fill
import net.neednot.renttracker.resources.ic_error_fill
import net.neednot.renttracker.resources.ic_schedule
import org.jetbrains.compose.resources.DrawableResource

@Composable
internal fun StatusChip(
    text: String,
    kind: StatusChipKind,
    modifier: Modifier = Modifier,
    icon: DrawableResource? = kind.defaultIcon(),
) {
    val colors = kind.colors()
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = colors.container,
        contentColor = colors.onContainer,
        border =
            if (kind == StatusChipKind.Neutral) {
                BorderStroke(Sizes.strokeThin, MaterialTheme.colorScheme.outline)
            } else {
                null
            },
    ) {
        Row(
            modifier = Modifier.heightIn(min = Sizes.chip).padding(horizontal = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            icon?.let { AppIcon(icon = it, contentDescription = null, size = IconSize.Small) }
            Text(text = text, maxLines = 1, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun StatusChipKind.colors(): ContainerColors =
    when (this) {
        StatusChipKind.Late -> {
            RentTrackerTheme.colors.statusLate
        }

        StatusChipKind.Owes -> {
            RentTrackerTheme.colors.statusOwes
        }

        StatusChipKind.PaidUp -> {
            RentTrackerTheme.colors.statusPaid
        }

        StatusChipKind.Neutral -> {
            ContainerColors(container = Color.Transparent, onContainer = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

private fun StatusChipKind.defaultIcon(): DrawableResource? =
    when (this) {
        StatusChipKind.Late -> Res.drawable.ic_error_fill
        StatusChipKind.Owes -> Res.drawable.ic_schedule
        StatusChipKind.PaidUp -> Res.drawable.ic_check_circle_fill
        StatusChipKind.Neutral -> null
    }
