package net.neednot.renttracker.core.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import net.neednot.renttracker.core.designsystem.theme.ContainerColors
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.designsystem.theme.Spacing
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ic_error
import net.neednot.renttracker.resources.ic_info
import net.neednot.renttracker.resources.ic_warning
import org.jetbrains.compose.resources.DrawableResource

@Composable
internal fun Banner(
    text: String,
    kind: BannerKind,
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: DrawableResource = kind.defaultIcon(),
    onClick: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val colors = kind.colors()
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
                .semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.medium,
        color = colors.container,
        contentColor = colors.onContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            AppIcon(icon = icon, contentDescription = null)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                title?.let { Text(text = it, style = MaterialTheme.typography.titleSmall) }
                Text(text = text, style = MaterialTheme.typography.bodyMedium)
            }
            action?.let { Box(modifier = Modifier.align(Alignment.CenterVertically)) { it() } }
        }
    }
}

@Composable
private fun BannerKind.colors(): ContainerColors {
    val scheme = MaterialTheme.colorScheme
    return when (this) {
        BannerKind.Warning -> {
            RentTrackerTheme.colors.statusOwes
        }

        BannerKind.Error -> {
            RentTrackerTheme.colors.statusLate
        }

        BannerKind.Info -> {
            ContainerColors(container = scheme.surfaceContainer, onContainer = scheme.onSurfaceVariant)
        }

        BannerKind.Note -> {
            ContainerColors(container = scheme.secondaryContainer, onContainer = scheme.onSecondaryContainer)
        }
    }
}

private fun BannerKind.defaultIcon(): DrawableResource =
    when (this) {
        BannerKind.Warning -> Res.drawable.ic_warning
        BannerKind.Error -> Res.drawable.ic_error
        BannerKind.Info, BannerKind.Note -> Res.drawable.ic_info
    }
