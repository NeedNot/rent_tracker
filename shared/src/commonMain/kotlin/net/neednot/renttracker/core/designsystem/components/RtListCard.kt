package net.neednot.renttracker.core.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun <T> RtListCard(
    items: List<T>,
    modifier: Modifier = Modifier,
    row: @Composable (T) -> Unit,
) {
    RtCard(modifier = modifier) { DividedRows(items = items, row = row) }
}
