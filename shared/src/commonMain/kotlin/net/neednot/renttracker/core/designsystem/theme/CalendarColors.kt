package net.neednot.renttracker.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class CalendarColors(
    val paid: CalendarStateColor,
    val partial: CalendarStateColor,
    val owed: CalendarStateColor,
    val dueNotLate: CalendarStateColor,
    val upcoming: CalendarStateColor,
    val prepaid: CalendarStateColor,
)

internal val LightCalendarColors =
    CalendarColors(
        paid = CalendarStateColor(container = Color(0xFF2E7D32), content = Color(0xFFFFFFFF)),
        partial = CalendarStateColor(container = Color(0xFFFFC233), content = Color(0xFF3D2E00)),
        owed = CalendarStateColor(container = Color(0xFFC62828), content = Color(0xFFFFFFFF)),
        dueNotLate =
            CalendarStateColor(
                container = Color.Transparent,
                content = Color(0xFF161D1D),
                border = Color(0xFF5A6B6A),
            ),
        upcoming = CalendarStateColor(container = Color(0xFFE3E9E8), content = Color(0xFF3F4948)),
        prepaid = CalendarStateColor(container = Color(0xFFC8EBC4), content = Color(0xFF14501A)),
    )

internal val DarkCalendarColors =
    CalendarColors(
        paid = CalendarStateColor(container = Color(0xFF7BCB7F), content = Color(0xFF08310C)),
        partial = CalendarStateColor(container = Color(0xFFF5C04A), content = Color(0xFF3D2E00)),
        owed = CalendarStateColor(container = Color(0xFFFF968A), content = Color(0xFF3B0907)),
        dueNotLate =
            CalendarStateColor(
                container = Color.Transparent,
                content = Color(0xFFDDE4E3),
                border = Color(0xFF9AA8A7),
            ),
        upcoming = CalendarStateColor(container = Color(0xFF3A4443), content = Color(0xFFDDE4E3)),
        prepaid = CalendarStateColor(container = Color(0xFF25452A), content = Color(0xFFC8EBC4)),
    )

internal val LocalCalendarColors = staticCompositionLocalOf { LightCalendarColors }
