package net.neednot.renttracker.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.designsystem.theme.Opacity
import net.neednot.renttracker.core.designsystem.theme.Spacing

private val ZeroCorner = CornerSize(0.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RtBottomSheet(
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismissed,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = MaterialTheme.shapes.extraLarge.copy(bottomStart = ZeroCorner, bottomEnd = ZeroCorner),
        containerColor = scheme.surface,
        tonalElevation = 0.dp,
        scrimColor = scheme.scrim.copy(alpha = Opacity.SCRIM),
        dragHandle = { SheetHandle() },
    ) {
        Column(
            modifier =
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(start = Spacing.xl, end = Spacing.xl, bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            content = content,
        )
    }
}

@Composable
private fun SheetHandle() {
    Box(
        modifier =
            Modifier
                .padding(top = Spacing.md, bottom = Spacing.xs + Spacing.md)
                .size(width = Spacing.xxxl, height = Spacing.xs)
                .background(MaterialTheme.colorScheme.outline, CircleShape),
    )
}
