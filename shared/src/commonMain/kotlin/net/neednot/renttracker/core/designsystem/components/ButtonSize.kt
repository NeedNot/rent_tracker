package net.neednot.renttracker.core.designsystem.components

import androidx.compose.ui.unit.Dp
import net.neednot.renttracker.core.designsystem.theme.Sizes
import net.neednot.renttracker.core.designsystem.theme.Spacing

enum class ButtonSize(
    val minHeight: Dp,
    val horizontalPadding: Dp,
) {
    Regular(minHeight = Sizes.touchTarget, horizontalPadding = Spacing.xxl),
    Small(minHeight = Sizes.buttonSmall, horizontalPadding = Spacing.xl),
}
