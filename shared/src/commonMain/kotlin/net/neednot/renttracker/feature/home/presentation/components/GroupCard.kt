package net.neednot.renttracker.feature.home.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import net.neednot.renttracker.core.designsystem.components.AppIcon
import net.neednot.renttracker.core.designsystem.components.IconSize
import net.neednot.renttracker.core.designsystem.components.RtCard
import net.neednot.renttracker.core.designsystem.components.RtDivider
import net.neednot.renttracker.core.designsystem.components.StatusChip
import net.neednot.renttracker.core.designsystem.components.StatusChipKind
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.designsystem.theme.Spacing
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.home.presentation.uistate.GroupCardUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ic_check_circle_fill
import net.neednot.renttracker.resources.ic_chevron_right
import net.neednot.renttracker.resources.ic_inventory_2

@Composable
fun GroupCard(
    group: GroupCardUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RtCard(
        modifier = modifier,
        onClick = onClick,
        contentPadding = PaddingValues(Spacing.lg),
        contentSpacing = Spacing.sm,
    ) {
        GroupName(name = group.name)
        GroupBalance(balance = group.balance, isPaidUp = group.isPaidUp)
        TenantCounts(tenantCount = group.tenantCount, lateCount = group.lateCount)
        group.archivedOwed?.let { archivedOwed -> ArchivedOwed(archivedOwed) }
    }
}

@Composable
private fun GroupName(name: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        AppIcon(
            icon = Res.drawable.ic_chevron_right,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GroupBalance(
    balance: UiText,
    isPaidUp: Boolean,
) {
    val color = if (isPaidUp) RentTrackerTheme.colors.statusPaid.onContainer else MaterialTheme.colorScheme.onSurface
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        if (isPaidUp) AppIcon(icon = Res.drawable.ic_check_circle_fill, contentDescription = null, tint = color)
        Text(text = balance.asString(), color = color, style = MaterialTheme.typography.headlineSmall)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TenantCounts(
    tenantCount: UiText,
    lateCount: UiText?,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = tenantCount.asString(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        lateCount?.let { count -> StatusChip(text = count.asString(), kind = StatusChipKind.Late) }
    }
}

@Composable
private fun ArchivedOwed(archivedOwed: UiText) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        RtDivider()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            AppIcon(
                icon = Res.drawable.ic_inventory_2,
                contentDescription = null,
                size = IconSize.Small,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = archivedOwed.asString(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Preview
@Composable
private fun GroupCardPreview() {
    RentTrackerTheme {
        GroupCard(
            group =
                GroupCardUiState(
                    groupId = "g1",
                    name = "1st Street",
                    balance = UiText.Plain("Owes $3,900.00"),
                    isPaidUp = false,
                    tenantCount = UiText.Plain("6 tenants"),
                    lateCount = UiText.Plain("2 late"),
                    archivedOwed = UiText.Plain("Archived tenants owe $2,400.00"),
                ),
            onClick = {},
        )
    }
}
