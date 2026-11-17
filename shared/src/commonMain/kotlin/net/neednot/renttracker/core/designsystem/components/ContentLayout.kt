package net.neednot.renttracker.core.designsystem.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import net.neednot.renttracker.core.designsystem.theme.Spacing

enum class ContentLayout(
    val padding: PaddingValues,
    val spacing: Dp,
) {
    List(
        padding = PaddingValues(start = Spacing.lg, top = Spacing.xs, end = Spacing.lg, bottom = Spacing.xxl),
        spacing = Spacing.md,
    ),
    Form(
        padding = PaddingValues(start = Spacing.lg, top = Spacing.xs, end = Spacing.lg, bottom = Spacing.xxl),
        spacing = Spacing.xl,
    ),
    Auth(
        padding = PaddingValues(start = Spacing.xxl, top = Spacing.sm, end = Spacing.xxl, bottom = Spacing.xxxl),
        spacing = Spacing.lg,
    ),
}
