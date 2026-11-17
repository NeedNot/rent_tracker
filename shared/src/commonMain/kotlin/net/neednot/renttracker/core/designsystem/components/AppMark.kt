package net.neednot.renttracker.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import net.neednot.renttracker.core.designsystem.theme.Sizes
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ic_home

@Composable
internal fun AppMark(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .size(Sizes.calendarSquare)
                .background(color = MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.large),
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(icon = Res.drawable.ic_home, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
    }
}
