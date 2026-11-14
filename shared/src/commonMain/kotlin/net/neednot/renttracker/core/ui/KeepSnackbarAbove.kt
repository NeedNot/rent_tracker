package net.neednot.renttracker.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp

val LocalSnackbarClearance = compositionLocalOf { SnackbarClearance() }

@Composable
fun KeepSnackbarAbove(bottomElementHeight: Dp) {
    val clearance = LocalSnackbarClearance.current
    val screen = remember { Any() }
    DisposableEffect(clearance, screen, bottomElementHeight) {
        clearance.keepClear(screen, bottomElementHeight)
        onDispose { clearance.release(screen) }
    }
}
