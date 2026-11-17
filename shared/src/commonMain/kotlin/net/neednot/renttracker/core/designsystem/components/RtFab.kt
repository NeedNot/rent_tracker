package net.neednot.renttracker.core.designsystem.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.designsystem.theme.Sizes
import org.jetbrains.compose.resources.DrawableResource

private val FabElevation = 3.dp

@Composable
internal fun RtFab(
    text: String,
    icon: DrawableResource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ExtendedFloatingActionButton(
        text = { Text(text) },
        icon = { AppIcon(icon = icon, contentDescription = null) },
        onClick = onClick,
        modifier = modifier.heightIn(min = Sizes.field),
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        elevation =
            FloatingActionButtonDefaults.elevation(
                defaultElevation = FabElevation,
                pressedElevation = FabElevation,
                focusedElevation = FabElevation,
                hoveredElevation = FabElevation,
            ),
    )
}
