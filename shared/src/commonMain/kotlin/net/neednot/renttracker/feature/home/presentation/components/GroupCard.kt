package net.neednot.renttracker.feature.home.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.home.presentation.uistate.GroupCardUiState

@Composable
fun GroupCard(
    group: GroupCardUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            GroupName(name = group.name)
            GroupBalance(balance = group.balance, isPaidUp = group.isPaidUp)
            TenantCounts(tenantCount = group.tenantCount, lateCount = group.lateCount)
            group.archivedOwed?.let { archivedOwed -> ArchivedOwed(archivedOwed) }
        }
    }
}

@Composable
private fun GroupName(name: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GroupBalance(
    balance: UiText,
    isPaidUp: Boolean,
) {
    val color = if (isPaidUp) RentTrackerTheme.calendarColors.prepaid.content else MaterialTheme.colorScheme.onSurface
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (isPaidUp) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = color,
            )
        }
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = tenantCount.asString(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        lateCount?.let { count -> LateChip(count) }
    }
}

@Composable
private fun LateChip(lateCount: UiText) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
            Text(text = lateCount.asString(), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun ArchivedOwed(archivedOwed: UiText) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                imageVector = Icons.Outlined.Inventory2,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
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
