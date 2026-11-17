package net.neednot.renttracker.feature.group.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import net.neednot.renttracker.core.designsystem.components.RowAmount
import net.neednot.renttracker.core.designsystem.components.RtListRow
import net.neednot.renttracker.core.designsystem.components.TenantStateChip
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.group.presentation.uistate.TenantRowUiState

@Composable
internal fun TenantRow(
    tenant: TenantRowUiState,
    isArchived: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RtListRow(
        title = tenant.name,
        modifier = modifier,
        subtitle = tenant.detail?.asString(),
        containerColor = if (isArchived) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent,
        onClick = onClick,
        trailing = {
            tenant.balance?.let { RowAmount(text = it.asString()) }
            TenantStateChip(state = tenant.chipState)
        },
    )
}
