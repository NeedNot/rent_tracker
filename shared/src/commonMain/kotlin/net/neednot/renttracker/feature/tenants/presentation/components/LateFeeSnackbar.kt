package net.neednot.renttracker.feature.tenants.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.designsystem.theme.Sizes
import net.neednot.renttracker.core.designsystem.theme.Spacing
import net.neednot.renttracker.core.ui.KeepSnackbarAbove
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.tenants.presentation.uistate.LateFeeSuggestionUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ledger_suggestion_add
import net.neednot.renttracker.resources.ledger_suggestion_dismiss
import org.jetbrains.compose.resources.stringResource

private val SnackbarElevation = 3.dp

@Composable
internal fun LateFeeSnackbar(
    suggestion: LateFeeSuggestionUiState,
    onDismissClicked: () -> Unit,
    onAddClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var height by remember { mutableStateOf(0.dp) }
    KeepSnackbarAbove(height)
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier =
            modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .onSizeChanged { height = with(density) { it.height.toDp() } }
                .padding(start = Spacing.sm, end = Spacing.sm, bottom = Spacing.lg)
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.small,
        color = scheme.inverseSurface,
        contentColor = scheme.inverseOnSurface,
        shadowElevation = SnackbarElevation,
    ) {
        Row(
            modifier = Modifier.heightIn(min = Sizes.touchTarget).padding(start = Spacing.lg, end = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                text = suggestion.message.asString(),
                modifier = Modifier.weight(1f).padding(vertical = Spacing.md),
                style = MaterialTheme.typography.bodyMedium,
            )
            SnackbarAction(text = stringResource(Res.string.ledger_suggestion_dismiss), onClick = onDismissClicked)
            SnackbarAction(text = stringResource(Res.string.ledger_suggestion_add), onClick = onAddClicked)
        }
    }
}

@Composable
private fun SnackbarAction(
    text: String,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick, modifier = Modifier.heightIn(min = Sizes.touchTarget)) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.inversePrimary,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
