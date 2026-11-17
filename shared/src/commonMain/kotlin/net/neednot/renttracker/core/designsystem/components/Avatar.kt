package net.neednot.renttracker.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import net.neednot.renttracker.core.designsystem.theme.ContainerColors
import net.neednot.renttracker.core.designsystem.theme.Sizes
import org.jetbrains.compose.resources.DrawableResource

@Composable
internal fun InitialsAvatar(
    initials: String,
    modifier: Modifier = Modifier,
    kind: AvatarKind = AvatarKind.Member,
    size: AvatarSize = AvatarSize.Regular,
) {
    Avatar(modifier = modifier, kind = kind, size = size) {
        val typography = MaterialTheme.typography
        Text(text = initials, style = if (size == AvatarSize.Small) typography.labelLarge else typography.titleMedium)
    }
}

@Composable
internal fun IconAvatar(
    icon: DrawableResource,
    modifier: Modifier = Modifier,
    kind: AvatarKind = AvatarKind.Member,
) {
    Avatar(modifier = modifier, kind = kind, size = AvatarSize.Regular) {
        AppIcon(icon = icon, contentDescription = null, size = IconSize.Small)
    }
}

@Composable
private fun Avatar(
    modifier: Modifier,
    kind: AvatarKind,
    size: AvatarSize,
    content: @Composable () -> Unit,
) {
    val colors = kind.colors()
    Box(
        modifier =
            modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(colors.container)
                .then(
                    if (kind == AvatarKind.Invite) {
                        Modifier.dashedBorder(Sizes.strokeThin, MaterialTheme.colorScheme.outline, size.dp / 2)
                    } else {
                        Modifier
                    },
                ).clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.onContainer, content = content)
    }
}

@Composable
private fun AvatarKind.colors(): ContainerColors {
    val scheme = MaterialTheme.colorScheme
    return when (this) {
        AvatarKind.Member -> {
            ContainerColors(container = scheme.secondaryContainer, onContainer = scheme.onSecondaryContainer)
        }

        AvatarKind.Owner -> {
            ContainerColors(container = scheme.primary, onContainer = scheme.onPrimary)
        }

        AvatarKind.Invite -> {
            ContainerColors(container = Color.Transparent, onContainer = scheme.onSurfaceVariant)
        }
    }
}
