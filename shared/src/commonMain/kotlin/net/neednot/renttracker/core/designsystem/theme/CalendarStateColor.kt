package net.neednot.renttracker.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class CalendarStateColor(
    val container: Color,
    val content: Color,
    val border: Color? = null,
)
