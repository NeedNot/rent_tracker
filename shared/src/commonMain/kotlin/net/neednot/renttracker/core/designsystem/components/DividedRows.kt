package net.neednot.renttracker.core.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun <T> DividedRows(
    items: List<T>,
    modifier: Modifier = Modifier,
    row: @Composable (T) -> Unit,
) {
    Column(modifier = modifier) {
        items.forEachIndexed { index, item ->
            if (index > 0) RtDivider()
            row(item)
        }
    }
}
