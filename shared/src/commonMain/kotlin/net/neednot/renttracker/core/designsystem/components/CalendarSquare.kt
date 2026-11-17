package net.neednot.renttracker.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.designsystem.theme.Sizes
import net.neednot.renttracker.core.designsystem.theme.Spacing

private val SquareCorner = 12.dp

@Composable
fun CalendarSquare(
    monthName: String,
    state: CalendarState,
    stateLabel: String,
    description: String,
    modifier: Modifier = Modifier,
    owed: String? = null,
    feeMarker: String? = null,
    position: CalendarPosition = CalendarPosition.OtherMonth,
    onClick: (() -> Unit)? = null,
) {
    val colors = state.colors()
    val shape = MaterialTheme.shapes.medium
    Box(
        modifier =
            modifier
                .clip(shape)
                .background(colors.container)
                .squareBorder(state, position)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .clearAndSetSemantics {
                    contentDescription = description
                    if (onClick != null) role = Role.Button
                },
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.onContainer) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = Sizes.calendarSquare)
                        .padding(
                            horizontal = Spacing.sm + Sizes.strokeThick,
                            vertical = Spacing.xs + Sizes.strokeThick,
                        ),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = monthName, style = MaterialTheme.typography.labelMedium)
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        AppIcon(icon = state.icon(), contentDescription = null, size = IconSize.Small)
                        Text(text = stateLabel, maxLines = 1, style = MaterialTheme.typography.labelSmall)
                    }
                    owed?.let { Text(text = it, maxLines = 1, style = MaterialTheme.typography.labelSmall) }
                }
            }
        }
        feeMarker?.let { FeeMarker(text = it, modifier = Modifier.align(Alignment.TopEnd)) }
    }
}

@Composable
private fun Modifier.squareBorder(
    state: CalendarState,
    position: CalendarPosition,
): Modifier {
    val shape = MaterialTheme.shapes.medium
    return when {
        position == CalendarPosition.CurrentMonth -> {
            border(Sizes.strokeThick, RentTrackerTheme.colors.calendarCurrentOutline, shape)
        }

        state == CalendarState.DueNotLate -> {
            border(Sizes.strokeThick, MaterialTheme.colorScheme.outline, shape)
        }

        state == CalendarState.NotATenant -> {
            dashedBorder(Sizes.strokeThin, MaterialTheme.colorScheme.outlineVariant, SquareCorner)
        }

        else -> {
            this
        }
    }
}

@Composable
private fun FeeMarker(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.padding(Spacing.xs + Sizes.strokeThick),
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.error,
        border = BorderStroke(Sizes.strokeThin, MaterialTheme.colorScheme.error),
    ) {
        Box(modifier = Modifier.heightIn(min = Sizes.tag).padding(horizontal = Spacing.xs)) {
            Text(
                text = text,
                modifier = Modifier.align(Alignment.Center),
                maxLines = 1,
                softWrap = false,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
