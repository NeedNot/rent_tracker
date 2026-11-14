package net.neednot.renttracker.feature.tenants.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.ui.KeepSnackbarAbove
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.tenants.presentation.uistate.LateFeeSuggestionUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ledger_suggestion_add
import net.neednot.renttracker.resources.ledger_suggestion_dismiss
import org.jetbrains.compose.resources.stringResource

private val EdgeMargin = 8.dp
private val BottomMargin = 16.dp
private val SnackbarCorner = 8.dp

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
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier =
            modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .onSizeChanged { height = with(density) { it.height.toDp() } }
                .padding(start = EdgeMargin, end = EdgeMargin, bottom = BottomMargin)
                .fillMaxWidth(),
        shape = RoundedCornerShape(SnackbarCorner),
        color = colors.inverseSurface,
        contentColor = colors.inverseOnSurface,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = suggestion.message.asString(),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onDismissClicked, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(text = stringResource(Res.string.ledger_suggestion_dismiss), color = colors.inverseOnSurface)
            }
            TextButton(onClick = onAddClicked, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(text = stringResource(Res.string.ledger_suggestion_add), color = colors.inversePrimary)
            }
        }
    }
}
