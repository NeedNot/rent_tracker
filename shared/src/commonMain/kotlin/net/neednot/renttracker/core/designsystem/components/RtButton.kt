package net.neednot.renttracker.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import net.neednot.renttracker.core.designsystem.theme.ContainerColors
import net.neednot.renttracker.core.designsystem.theme.Opacity
import net.neednot.renttracker.core.designsystem.theme.Sizes
import net.neednot.renttracker.core.designsystem.theme.Spacing
import org.jetbrains.compose.resources.DrawableResource

@Composable
internal fun RtButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Filled,
    tone: ActionTone = ActionTone.Default,
    size: ButtonSize = ButtonSize.Regular,
    enabled: Boolean = true,
    isWorking: Boolean = false,
    leadingIcon: DrawableResource? = null,
) {
    val colors = variant.colors(tone)
    Button(
        onClick = { if (!isWorking) onClick() },
        modifier = modifier.heightIn(min = size.minHeight).alpha(if (enabled) 1f else Opacity.DISABLED),
        enabled = enabled,
        shape = CircleShape,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = colors.container,
                contentColor = colors.onContainer,
                disabledContainerColor = colors.container,
                disabledContentColor = colors.onContainer,
            ),
        border = variant.borderColor(tone)?.let { BorderStroke(Sizes.strokeThin, it) },
        contentPadding =
            PaddingValues(horizontal = if (variant == ButtonVariant.Text) Spacing.md else size.horizontalPadding),
    ) {
        when {
            isWorking -> ButtonSpinner()
            leadingIcon != null -> AppIcon(icon = leadingIcon, contentDescription = null, size = IconSize.Small)
        }
        if (isWorking || leadingIcon != null) Spacer(Modifier.width(Spacing.sm))
        Text(text = text, maxLines = 1)
    }
}

@Composable
private fun ButtonSpinner() {
    CircularProgressIndicator(
        modifier = Modifier.size(Sizes.iconSmall),
        color = LocalContentColor.current,
        strokeWidth = Sizes.strokeThick,
    )
}

@Composable
private fun ButtonVariant.colors(tone: ActionTone): ContainerColors {
    val scheme = MaterialTheme.colorScheme
    val accent = if (tone == ActionTone.Destructive) scheme.error else scheme.primary
    return when (this) {
        ButtonVariant.Filled -> {
            ContainerColors(container = scheme.primary, onContainer = scheme.onPrimary)
        }

        ButtonVariant.Tonal -> {
            ContainerColors(container = scheme.secondaryContainer, onContainer = scheme.onSecondaryContainer)
        }

        ButtonVariant.Outlined -> {
            ContainerColors(container = scheme.surfaceContainerLow, onContainer = accent)
        }

        ButtonVariant.Text -> {
            ContainerColors(container = Color.Transparent, onContainer = accent)
        }
    }
}

@Composable
private fun ButtonVariant.borderColor(tone: ActionTone): Color? {
    if (this != ButtonVariant.Outlined) return null
    return if (tone == ActionTone.Destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
}
