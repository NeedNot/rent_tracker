package net.neednot.renttracker.feature.tenants.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.feature.tenants.presentation.uistate.EntryChip
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ledger_chip_bounced
import net.neednot.renttracker.resources.ledger_chip_cancelled
import net.neednot.renttracker.resources.ledger_chip_edited
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun EntryChipLabel(
    chip: EntryChip,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = chip.containerColor(),
        contentColor = chip.contentColor(),
    ) {
        Text(
            text = stringResource(chip.label()),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun EntryChip.containerColor(): Color =
    when (this) {
        EntryChip.Edited -> MaterialTheme.colorScheme.surfaceContainerHigh
        EntryChip.Bounced, EntryChip.Cancelled -> MaterialTheme.colorScheme.errorContainer
    }

@Composable
private fun EntryChip.contentColor(): Color =
    when (this) {
        EntryChip.Edited -> MaterialTheme.colorScheme.onSurfaceVariant
        EntryChip.Bounced, EntryChip.Cancelled -> MaterialTheme.colorScheme.onErrorContainer
    }

private fun EntryChip.label(): StringResource =
    when (this) {
        EntryChip.Edited -> Res.string.ledger_chip_edited
        EntryChip.Bounced -> Res.string.ledger_chip_bounced
        EntryChip.Cancelled -> Res.string.ledger_chip_cancelled
    }
