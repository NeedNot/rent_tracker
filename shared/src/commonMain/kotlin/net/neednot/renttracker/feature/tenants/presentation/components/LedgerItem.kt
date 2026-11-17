package net.neednot.renttracker.feature.tenants.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.neednot.renttracker.core.designsystem.components.ButtonVariant
import net.neednot.renttracker.core.designsystem.components.RtButton
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerItemUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerRowUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingRowUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ledger_restore
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LedgerItem(
    item: LedgerItemUiState,
    actions: LedgerItemActions,
    modifier: Modifier = Modifier,
) {
    when (item) {
        is LedgerItemUiState.Entry -> EntryRow(item.row, actions, modifier)
        is LedgerItemUiState.Pending -> PendingRow(item.row, actions, modifier)
    }
}

@Composable
private fun EntryRow(
    row: LedgerRowUiState,
    actions: LedgerItemActions,
    modifier: Modifier,
) {
    val details = row.details.map { it.asString() }
    val hasMeta = row.chips.isNotEmpty() || row.mayNotApply || details.isNotEmpty()
    LedgerRow(
        date = row.date,
        title = row.title.asString(),
        amount = row.amount.asString(),
        onClick = if (row.isTappable) ({ actions.onEntryClicked(row.key) }) else null,
        modifier = modifier,
        style = if (row.isStruckThrough) LedgerRowStyle.Void else LedgerRowStyle.Normal,
        isPaidAmount = row.isPaidAmount,
        balance = row.balance?.asString(),
        meta =
            if (hasMeta) {
                {
                    row.chips.forEach { chip -> EntryTag(chip) }
                    if (row.mayNotApply) MayNotApplyTag()
                    details.forEach { detail -> LedgerMeta(detail) }
                }
            } else {
                null
            },
        endAction =
            if (row.canRestore) {
                {
                    RtButton(
                        text = stringResource(Res.string.ledger_restore),
                        onClick = { actions.onRestoreClicked(row.key) },
                        variant = ButtonVariant.Text,
                    )
                }
            } else {
                null
            },
    )
}

@Composable
private fun PendingRow(
    row: PendingRowUiState,
    actions: LedgerItemActions,
    modifier: Modifier,
) {
    val detail = row.detail.asString()
    LedgerRow(
        date = row.date,
        title = row.title.asString(),
        amount = row.amount.asString(),
        onClick = { actions.onPendingClicked(row.clientId) },
        modifier = modifier,
        style = LedgerRowStyle.Inactive,
        meta = {
            PendingTag(row.chip)
            LedgerMeta(detail)
        },
    )
}
