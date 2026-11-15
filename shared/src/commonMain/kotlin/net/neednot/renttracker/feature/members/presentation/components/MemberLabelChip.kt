package net.neednot.renttracker.feature.members.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.access.MemberPermissionLabel
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MemberLabelChip(
    label: MemberPermissionLabel,
    modifier: Modifier = Modifier,
) {
    val isOwner = label == MemberPermissionLabel.Owner
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = if (isOwner) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        border = if (isOwner) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
    ) {
        Text(
            text = stringResource(label.text),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            maxLines = 1,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
