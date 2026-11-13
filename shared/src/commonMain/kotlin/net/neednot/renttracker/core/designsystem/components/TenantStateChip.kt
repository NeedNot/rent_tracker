package net.neednot.renttracker.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.balance_paid_up
import net.neednot.renttracker.resources.tenant_state_late
import net.neednot.renttracker.resources.tenant_state_owes
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun TenantStateChip(
    state: TenantChipState,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = state.containerColor(),
        contentColor = state.contentColor(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(imageVector = state.icon(), contentDescription = null, modifier = Modifier.size(14.dp))
            Text(text = stringResource(state.label()), maxLines = 1, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun TenantChipState.containerColor(): Color =
    when (this) {
        TenantChipState.Late -> MaterialTheme.colorScheme.errorContainer
        TenantChipState.Owes -> RentTrackerTheme.calendarColors.partial.container
        TenantChipState.PaidUp -> RentTrackerTheme.calendarColors.prepaid.container
    }

@Composable
private fun TenantChipState.contentColor(): Color =
    when (this) {
        TenantChipState.Late -> MaterialTheme.colorScheme.onErrorContainer
        TenantChipState.Owes -> RentTrackerTheme.calendarColors.partial.content
        TenantChipState.PaidUp -> RentTrackerTheme.calendarColors.prepaid.content
    }

private fun TenantChipState.icon(): ImageVector =
    when (this) {
        TenantChipState.Late -> Icons.Outlined.ErrorOutline
        TenantChipState.Owes -> Icons.Outlined.Schedule
        TenantChipState.PaidUp -> Icons.Filled.Check
    }

private fun TenantChipState.label(): StringResource =
    when (this) {
        TenantChipState.Late -> Res.string.tenant_state_late
        TenantChipState.Owes -> Res.string.tenant_state_owes
        TenantChipState.PaidUp -> Res.string.balance_paid_up
    }

@Preview
@Composable
private fun TenantStateChipPreview() {
    RentTrackerTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TenantChipState.entries.forEach { state -> TenantStateChip(state = state) }
        }
    }
}
