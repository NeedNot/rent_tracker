package net.neednot.renttracker.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import net.neednot.renttracker.core.designsystem.theme.Opacity
import net.neednot.renttracker.core.designsystem.theme.Sizes
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ic_check

@Composable
internal fun RtSegmentedButton(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth().alpha(if (enabled) 1f else Opacity.DISABLED)) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = index == selectedIndex,
                onClick = { onSelected(index) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                modifier = Modifier.heightIn(min = Sizes.touchTarget),
                enabled = enabled,
                colors =
                    SegmentedButtonDefaults.colors(
                        activeContainerColor = scheme.secondaryContainer,
                        activeContentColor = scheme.onSecondaryContainer,
                        activeBorderColor = scheme.outline,
                        inactiveContentColor = scheme.onSurface,
                        inactiveBorderColor = scheme.outline,
                        disabledActiveContainerColor = scheme.secondaryContainer,
                        disabledActiveContentColor = scheme.onSecondaryContainer,
                        disabledActiveBorderColor = scheme.outline,
                        disabledInactiveContentColor = scheme.onSurface,
                        disabledInactiveBorderColor = scheme.outline,
                    ),
                border = BorderStroke(Sizes.strokeThin, scheme.outline),
                icon = {
                    SegmentedButtonDefaults.Icon(active = index == selectedIndex) {
                        AppIcon(icon = Res.drawable.ic_check, contentDescription = null, size = IconSize.Small)
                    }
                },
            ) {
                Text(text = option, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
