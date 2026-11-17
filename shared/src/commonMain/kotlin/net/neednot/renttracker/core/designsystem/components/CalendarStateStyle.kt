package net.neednot.renttracker.core.designsystem.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import net.neednot.renttracker.core.designsystem.theme.ContainerColors
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ic_check_circle
import net.neednot.renttracker.resources.ic_check_circle_fill
import net.neednot.renttracker.resources.ic_contrast
import net.neednot.renttracker.resources.ic_error_fill
import net.neednot.renttracker.resources.ic_event
import net.neednot.renttracker.resources.ic_remove
import net.neednot.renttracker.resources.ic_schedule
import org.jetbrains.compose.resources.DrawableResource

internal fun CalendarState.icon(): DrawableResource =
    when (this) {
        CalendarState.Paid -> Res.drawable.ic_check_circle_fill
        CalendarState.Partial -> Res.drawable.ic_contrast
        CalendarState.Owed -> Res.drawable.ic_error_fill
        CalendarState.DueNotLate -> Res.drawable.ic_schedule
        CalendarState.Upcoming -> Res.drawable.ic_event
        CalendarState.Prepaid -> Res.drawable.ic_check_circle
        CalendarState.NotATenant -> Res.drawable.ic_remove
    }

@Composable
internal fun CalendarState.colors(): ContainerColors {
    val scheme = MaterialTheme.colorScheme
    return when (this) {
        CalendarState.Paid, CalendarState.Prepaid -> {
            RentTrackerTheme.colors.statusPaid
        }

        CalendarState.Partial -> {
            RentTrackerTheme.colors.statusOwes
        }

        CalendarState.Owed -> {
            RentTrackerTheme.colors.statusLate
        }

        CalendarState.DueNotLate -> {
            ContainerColors(container = scheme.surfaceContainerLow, onContainer = scheme.onSurface)
        }

        CalendarState.Upcoming -> {
            ContainerColors(container = scheme.surfaceContainer, onContainer = scheme.onSurfaceVariant)
        }

        CalendarState.NotATenant -> {
            ContainerColors(container = Color.Transparent, onContainer = scheme.onSurfaceVariant)
        }
    }
}

internal fun CalendarState.chipKind(): StatusChipKind =
    when (this) {
        CalendarState.Paid, CalendarState.Prepaid -> StatusChipKind.PaidUp
        CalendarState.Partial -> StatusChipKind.Owes
        CalendarState.Owed -> StatusChipKind.Late
        CalendarState.DueNotLate, CalendarState.Upcoming, CalendarState.NotATenant -> StatusChipKind.Neutral
    }
