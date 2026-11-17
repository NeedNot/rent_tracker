package net.neednot.renttracker.feature.tenants.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import net.neednot.renttracker.core.data.TenantBalanceState
import net.neednot.renttracker.core.designsystem.components.Banner
import net.neednot.renttracker.core.designsystem.components.BannerKind
import net.neednot.renttracker.core.designsystem.components.ButtonVariant
import net.neednot.renttracker.core.designsystem.components.RtButton
import net.neednot.renttracker.core.designsystem.components.RtCard
import net.neednot.renttracker.core.designsystem.components.RtDivider
import net.neednot.renttracker.core.designsystem.components.TenantChipState
import net.neednot.renttracker.core.designsystem.components.TenantStateChip
import net.neednot.renttracker.core.designsystem.theme.Spacing
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.tenants.presentation.uistate.BalanceLineUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantBalanceUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ic_add
import net.neednot.renttracker.resources.ledger_add_charge
import net.neednot.renttracker.resources.ledger_log_payment
import net.neednot.renttracker.resources.ledger_total_balance
import org.jetbrains.compose.resources.stringResource

private const val SPLIT_COLUMNS = 3
private const val ACTIONS_PER_ROW = 2

@Composable
internal fun BalanceCard(
    balance: TenantBalanceUiState?,
    canLogPayment: Boolean,
    canAddCharge: Boolean,
    onLogPaymentClicked: () -> Unit,
    onAddChargeClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (balance == null && !canLogPayment && !canAddCharge) return
    RtCard(
        modifier = modifier,
        contentPadding = PaddingValues(Spacing.lg),
        contentSpacing = Spacing.md,
    ) {
        balance?.let { BalanceSummary(it) }
        BalanceActions(canLogPayment, canAddCharge, onLogPaymentClicked, onAddChargeClicked)
    }
}

@Composable
private fun BalanceSummary(balance: TenantBalanceUiState) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.ledger_total_balance),
            modifier = Modifier.weight(1f),
            color = scheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleSmall,
        )
        TenantStateChip(balance.state.toChipState())
    }
    Text(text = balance.total.asString(), style = MaterialTheme.typography.displaySmall)
    if (balance.lines.isNotEmpty()) BalanceSplit(balance.lines)
    balance.nextDue?.let { nextDue ->
        Text(text = nextDue.asString(), color = scheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
    balance.partialMoveOutWarning?.let { warning ->
        Banner(text = warning.asString(), kind = BannerKind.Warning)
    }
}

@Composable
private fun BalanceSplit(lines: List<BalanceLineUiState>) {
    Column {
        RtDivider()
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            lines.take(SPLIT_COLUMNS).forEach { line -> BalanceLine(line, Modifier.weight(1f)) }
            repeat(SPLIT_COLUMNS - lines.size.coerceAtMost(SPLIT_COLUMNS)) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun BalanceLine(
    line: BalanceLineUiState,
    modifier: Modifier,
) {
    Column(modifier = modifier.semantics(mergeDescendants = true) {}) {
        Text(
            text = line.label.asString(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(text = line.amount, style = MaterialTheme.typography.titleMedium)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BalanceActions(
    canLogPayment: Boolean,
    canAddCharge: Boolean,
    onLogPaymentClicked: () -> Unit,
    onAddChargeClicked: () -> Unit,
) {
    if (!canLogPayment && !canAddCharge) return
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        maxItemsInEachRow = ACTIONS_PER_ROW,
    ) {
        if (canLogPayment) {
            RtButton(
                text = stringResource(Res.string.ledger_log_payment),
                onClick = onLogPaymentClicked,
                modifier = Modifier.weight(1f),
                leadingIcon = Res.drawable.ic_add,
            )
        }
        if (canAddCharge) {
            RtButton(
                text = stringResource(Res.string.ledger_add_charge),
                onClick = onAddChargeClicked,
                modifier = Modifier.weight(1f),
                variant = ButtonVariant.Tonal,
            )
        }
    }
}

private fun TenantBalanceState.toChipState(): TenantChipState =
    when (this) {
        TenantBalanceState.Late -> TenantChipState.Late
        TenantBalanceState.Owes -> TenantChipState.Owes
        TenantBalanceState.PaidUp -> TenantChipState.PaidUp
    }
