package net.neednot.renttracker.feature.auth.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.asString

@Composable
fun AuthErrorText(
    error: UiText,
    modifier: Modifier = Modifier,
) {
    Text(
        text = error.asString(),
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
    )
}
