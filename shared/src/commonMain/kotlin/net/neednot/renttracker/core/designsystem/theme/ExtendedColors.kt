package net.neednot.renttracker.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ExtendedColors(
    val borderCard: Color,
    val borderDivider: Color,
    val entryInactive: ContainerColors,
    val statusLate: ContainerColors,
    val statusOwes: ContainerColors,
    val statusPaid: ContainerColors,
    val statusPending: ContainerColors,
    val calendarCurrentOutline: Color,
)

internal val LightExtendedColors =
    ExtendedColors(
        borderCard = Color(0xFFDAE4E3),
        borderDivider = Color(0xFFE3E9E8),
        entryInactive = ContainerColors(container = Color(0xFFF4F6F6), onContainer = Color(0xFF5B6463)),
        statusLate = ContainerColors(container = Color(0xFFFFDAD6), onContainer = Color(0xFF8C1D18)),
        statusOwes = ContainerColors(container = Color(0xFFFFE0A3), onContainer = Color(0xFF5A3B00)),
        statusPaid = ContainerColors(container = Color(0xFFCDEBD0), onContainer = Color(0xFF1E5128)),
        statusPending =
            ContainerColors(
                container = LightColorScheme.surfaceContainerHigh,
                onContainer = LightColorScheme.onSurfaceVariant,
            ),
        calendarCurrentOutline = Color(0xFF8A5A00),
    )

internal val DarkExtendedColors =
    ExtendedColors(
        borderCard = Color(0xFF2E3535),
        borderDivider = Color(0xFF2A3131),
        entryInactive = ContainerColors(container = Color(0xFF161D1D), onContainer = Color(0xFF899392)),
        statusLate = ContainerColors(container = Color(0xFF93000A), onContainer = Color(0xFFFFDAD6)),
        statusOwes = ContainerColors(container = Color(0xFF5C4200), onContainer = Color(0xFFFFDEA6)),
        statusPaid = ContainerColors(container = Color(0xFF1D5221), onContainer = Color(0xFFB2E7BC)),
        statusPending =
            ContainerColors(
                container = DarkColorScheme.surfaceContainerHigh,
                onContainer = DarkColorScheme.onSurfaceVariant,
            ),
        calendarCurrentOutline = Color(0xFFF6C25B),
    )

internal val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }
