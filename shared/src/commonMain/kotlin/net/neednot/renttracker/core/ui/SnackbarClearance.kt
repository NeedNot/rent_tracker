package net.neednot.renttracker.core.ui

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

class SnackbarClearance {
    private val heightsByScreen = mutableStateMapOf<Any, Dp>()

    val height: Dp get() = heightsByScreen.values.maxOrNull() ?: 0.dp

    internal fun keepClear(
        screen: Any,
        height: Dp,
    ) {
        heightsByScreen[screen] = height
    }

    internal fun release(screen: Any) {
        heightsByScreen.remove(screen)
    }
}
