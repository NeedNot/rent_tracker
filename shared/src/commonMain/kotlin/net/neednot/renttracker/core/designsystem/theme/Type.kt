package net.neednot.renttracker.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle

private const val TABULAR_NUMBERS = "tnum"

private fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = TABULAR_NUMBERS)

// The design's type scale is the stock Material 3 scale; only tabular figures are added so money lines up.
internal val RentTrackerTypography =
    Typography().run {
        copy(
            displayLarge = displayLarge.tabular(),
            displayMedium = displayMedium.tabular(),
            displaySmall = displaySmall.tabular(),
            headlineLarge = headlineLarge.tabular(),
            headlineMedium = headlineMedium.tabular(),
            headlineSmall = headlineSmall.tabular(),
            titleLarge = titleLarge.tabular(),
            titleMedium = titleMedium.tabular(),
            titleSmall = titleSmall.tabular(),
            bodyLarge = bodyLarge.tabular(),
            bodyMedium = bodyMedium.tabular(),
            bodySmall = bodySmall.tabular(),
            labelLarge = labelLarge.tabular(),
            labelMedium = labelMedium.tabular(),
            labelSmall = labelSmall.tabular(),
        )
    }
