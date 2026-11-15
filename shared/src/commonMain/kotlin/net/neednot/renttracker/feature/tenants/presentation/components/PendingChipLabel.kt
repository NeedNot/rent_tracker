package net.neednot.renttracker.feature.tenants.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingChip
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.pending_chip_needs_confirmation
import net.neednot.renttracker.resources.pending_chip_pending
import net.neednot.renttracker.resources.pending_chip_rejected
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PendingChipLabel(
    chip: PendingChip,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = chip.containerColor(),
        contentColor = chip.contentColor(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (chip == PendingChip.Pending) {
                Icon(
                    imageVector = Icons.Outlined.Smartphone,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                )
            }
            Text(text = stringResource(chip.label()), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun PendingChip.containerColor(): Color =
    when (this) {
        PendingChip.Pending -> MaterialTheme.colorScheme.surfaceContainerHigh
        PendingChip.NeedsConfirmation -> MaterialTheme.colorScheme.tertiaryContainer
        PendingChip.Rejected -> MaterialTheme.colorScheme.errorContainer
    }

@Composable
private fun PendingChip.contentColor(): Color =
    when (this) {
        PendingChip.Pending -> MaterialTheme.colorScheme.onSurfaceVariant
        PendingChip.NeedsConfirmation -> MaterialTheme.colorScheme.onTertiaryContainer
        PendingChip.Rejected -> MaterialTheme.colorScheme.onErrorContainer
    }

private fun PendingChip.label(): StringResource =
    when (this) {
        PendingChip.Pending -> Res.string.pending_chip_pending
        PendingChip.NeedsConfirmation -> Res.string.pending_chip_needs_confirmation
        PendingChip.Rejected -> Res.string.pending_chip_rejected
    }
