package net.neednot.renttracker.core.designsystem.components

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.designsystem.theme.Sizes

@Composable
internal fun RtDivider(
    modifier: Modifier = Modifier,
    color: Color = RentTrackerTheme.colors.borderDivider,
) {
    HorizontalDivider(modifier = modifier, thickness = Sizes.strokeThin, color = color)
}
