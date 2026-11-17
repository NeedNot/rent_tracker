package net.neednot.renttracker.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.designsystem.theme.Spacing
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
    StatusChip(text = stringResource(state.label()), kind = state.kind(), modifier = modifier)
}

private fun TenantChipState.kind(): StatusChipKind =
    when (this) {
        TenantChipState.Late -> StatusChipKind.Late
        TenantChipState.Owes -> StatusChipKind.Owes
        TenantChipState.PaidUp -> StatusChipKind.PaidUp
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
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            TenantChipState.entries.forEach { state -> TenantStateChip(state = state) }
        }
    }
}
