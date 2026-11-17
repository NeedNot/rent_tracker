package net.neednot.renttracker.feature.tenants.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.neednot.renttracker.core.designsystem.components.ActionTone
import net.neednot.renttracker.core.designsystem.components.DetailLayout
import net.neednot.renttracker.core.designsystem.components.DetailRow
import net.neednot.renttracker.core.designsystem.components.SheetAction
import net.neednot.renttracker.core.designsystem.components.SheetActions
import net.neednot.renttracker.core.designsystem.components.SheetHeader
import net.neednot.renttracker.core.designsystem.theme.Spacing
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerSheetAction
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerSheetUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ic_block
import net.neednot.renttracker.resources.ic_edit
import net.neednot.renttracker.resources.ic_restore
import net.neednot.renttracker.resources.ic_undo
import net.neednot.renttracker.resources.ledger_action_cancel_charge
import net.neednot.renttracker.resources.ledger_action_cancel_payment
import net.neednot.renttracker.resources.ledger_action_edit
import net.neednot.renttracker.resources.ledger_action_mark_bounced
import net.neednot.renttracker.resources.ledger_action_restore_clear_bounce
import net.neednot.renttracker.resources.ledger_restore
import net.neednot.renttracker.resources.ledger_view_only
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LedgerEntrySheet(
    sheet: LedgerSheetUiState,
    isWorking: Boolean,
    onActionClicked: (LedgerSheetAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        SheetHeader(
            label = sheet.title.asString(),
            amount = sheet.amount,
            trailing =
                if (sheet.chips.isEmpty()) {
                    null
                } else {
                    {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        ) { sheet.chips.forEach { EntryTag(it) } }
                    }
                },
        )
        Column {
            sheet.fields.forEach { field ->
                DetailRow(label = field.label.asString(), value = field.value.asString(), layout = DetailLayout.Inline)
            }
        }
        if (sheet.isViewOnly) {
            Text(
                text = stringResource(Res.string.ledger_view_only),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            SheetActions {
                sheet.actions.forEach { action ->
                    SheetAction(
                        text = stringResource(action.label()),
                        icon = action.icon(),
                        onClick = { onActionClicked(action) },
                        tone = if (action.isDestructive()) ActionTone.Destructive else ActionTone.Default,
                        enabled = !isWorking,
                    )
                }
            }
        }
    }
}

private fun LedgerSheetAction.isDestructive(): Boolean =
    this == LedgerSheetAction.CancelPayment || this == LedgerSheetAction.CancelCharge

private fun LedgerSheetAction.icon(): DrawableResource =
    when (this) {
        LedgerSheetAction.Edit -> Res.drawable.ic_edit
        LedgerSheetAction.MarkBounced -> Res.drawable.ic_undo
        LedgerSheetAction.CancelPayment, LedgerSheetAction.CancelCharge -> Res.drawable.ic_block
        LedgerSheetAction.Restore, LedgerSheetAction.RestoreClearBounce -> Res.drawable.ic_restore
    }

private fun LedgerSheetAction.label(): StringResource =
    when (this) {
        LedgerSheetAction.Edit -> Res.string.ledger_action_edit
        LedgerSheetAction.MarkBounced -> Res.string.ledger_action_mark_bounced
        LedgerSheetAction.CancelPayment -> Res.string.ledger_action_cancel_payment
        LedgerSheetAction.CancelCharge -> Res.string.ledger_action_cancel_charge
        LedgerSheetAction.Restore -> Res.string.ledger_restore
        LedgerSheetAction.RestoreClearBounce -> Res.string.ledger_action_restore_clear_bounce
    }
