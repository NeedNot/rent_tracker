package net.neednot.renttracker.core.designsystem.components

import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.calendar_state_due_not_late
import net.neednot.renttracker.resources.calendar_state_not_a_tenant
import net.neednot.renttracker.resources.calendar_state_owed
import net.neednot.renttracker.resources.calendar_state_paid
import net.neednot.renttracker.resources.calendar_state_partial
import net.neednot.renttracker.resources.calendar_state_prepaid
import net.neednot.renttracker.resources.calendar_state_upcoming
import org.jetbrains.compose.resources.StringResource

fun CalendarState.label(): StringResource =
    when (this) {
        CalendarState.Paid -> Res.string.calendar_state_paid
        CalendarState.Partial -> Res.string.calendar_state_partial
        CalendarState.Owed -> Res.string.calendar_state_owed
        CalendarState.DueNotLate -> Res.string.calendar_state_due_not_late
        CalendarState.Upcoming -> Res.string.calendar_state_upcoming
        CalendarState.Prepaid -> Res.string.calendar_state_prepaid
        CalendarState.NotATenant -> Res.string.calendar_state_not_a_tenant
    }
