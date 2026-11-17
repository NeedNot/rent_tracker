package net.neednot.renttracker.core.designsystem.components

import androidx.compose.ui.unit.Dp
import net.neednot.renttracker.core.designsystem.theme.Sizes

enum class SearchBarSize(
    val minHeight: Dp,
) {
    Full(Sizes.field),
    Compact(Sizes.touchTarget),
}
