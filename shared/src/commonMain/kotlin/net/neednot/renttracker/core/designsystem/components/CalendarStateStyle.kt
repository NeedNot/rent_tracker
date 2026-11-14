package net.neednot.renttracker.core.designsystem.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import net.neednot.renttracker.core.designsystem.theme.CalendarStateColor
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme

internal fun CalendarState.icon(): ImageVector? =
    when (this) {
        CalendarState.Paid -> Icons.Filled.Check
        CalendarState.Partial -> Icons.Filled.Contrast
        CalendarState.Owed -> Icons.Outlined.ErrorOutline
        CalendarState.DueNotLate -> Icons.Outlined.Schedule
        CalendarState.Upcoming -> Icons.Outlined.Circle
        CalendarState.Prepaid -> Icons.Outlined.CheckCircle
        CalendarState.NotATenant -> null
    }

private const val DISABLED_ALPHA = 0.38f

@Composable
internal fun CalendarState.colors(): CalendarStateColor {
    val colors = RentTrackerTheme.calendarColors
    return when (this) {
        CalendarState.Paid -> colors.paid
        CalendarState.Partial -> colors.partial
        CalendarState.Owed -> colors.owed
        CalendarState.DueNotLate -> colors.dueNotLate
        CalendarState.Upcoming -> colors.upcoming
        CalendarState.Prepaid -> colors.prepaid
        CalendarState.NotATenant -> notATenantColors()
    }
}

@Composable
private fun notATenantColors(): CalendarStateColor =
    CalendarStateColor(
        container = Color.Transparent,
        content = MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ALPHA),
        border = MaterialTheme.colorScheme.outlineVariant.copy(alpha = DISABLED_ALPHA),
    )
