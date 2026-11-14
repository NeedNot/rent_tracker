package net.neednot.renttracker.feature.tenants.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.data.TenantBalanceState
import net.neednot.renttracker.core.designsystem.components.SectionCard
import net.neednot.renttracker.core.designsystem.components.TenantChipState
import net.neednot.renttracker.core.designsystem.components.TenantStateChip
import net.neednot.renttracker.core.designsystem.components.WarningNotice
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.tenants.presentation.uistate.BalanceLineUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantBalanceUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ledger_total_balance
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BalanceCard(
    balance: TenantBalanceUiState,
    modifier: Modifier = Modifier,
) {
    SectionCard(heading = stringResource(Res.string.ledger_total_balance), modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = balance.total.asString(), style = MaterialTheme.typography.headlineMedium)
                TenantStateChip(balance.state.toChipState())
            }
            balance.lines.forEach { line -> BalanceLine(line) }
            balance.nextDue?.let { nextDue ->
                Text(
                    text = nextDue.asString(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            balance.partialMoveOutWarning?.let { warning -> WarningNotice(warning.asString()) }
        }
    }
}

@Composable
private fun BalanceLine(line: BalanceLineUiState) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = line.label.asString(),
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(text = line.amount, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun TenantBalanceState.toChipState(): TenantChipState =
    when (this) {
        TenantBalanceState.Late -> TenantChipState.Late
        TenantBalanceState.Owes -> TenantChipState.Owes
        TenantBalanceState.PaidUp -> TenantChipState.PaidUp
    }
