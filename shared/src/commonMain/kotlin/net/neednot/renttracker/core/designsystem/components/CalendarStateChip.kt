package net.neednot.renttracker.core.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CalendarStateChip(
    state: CalendarState,
    label: String,
    modifier: Modifier = Modifier,
) {
    StatusChip(text = label, kind = state.chipKind(), modifier = modifier, icon = state.icon())
}
