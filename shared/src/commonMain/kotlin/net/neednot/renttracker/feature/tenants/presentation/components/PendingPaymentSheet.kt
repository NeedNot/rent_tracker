package net.neednot.renttracker.feature.tenants.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.neednot.renttracker.core.designsystem.components.ActionTone
import net.neednot.renttracker.core.designsystem.components.SheetAction
import net.neednot.renttracker.core.designsystem.components.SheetActions
import net.neednot.renttracker.core.designsystem.components.SheetHeader
import net.neednot.renttracker.core.designsystem.theme.Spacing
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingSheetAction
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingSheetUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ic_block
import net.neednot.renttracker.resources.ic_check
import net.neednot.renttracker.resources.ic_edit
import net.neednot.renttracker.resources.ic_send
import net.neednot.renttracker.resources.ledger_action_cancel_payment
import net.neednot.renttracker.resources.ledger_action_edit
import net.neednot.renttracker.resources.pending_action_discard
import net.neednot.renttracker.resources.pending_action_save_anyway
import net.neednot.renttracker.resources.pending_action_send_anyway
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PendingPaymentSheet(
    sheet: PendingSheetUiState,
    isWorking: Boolean,
    onActionClicked: (PendingSheetAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        SheetHeader(label = sheet.title.asString(), amount = sheet.amount, trailing = { PendingTag(sheet.chip) })
        PendingSheetBodyContent(sheet.body)
        if (sheet.actions.isNotEmpty()) {
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

private fun PendingSheetAction.isDestructive(): Boolean =
    this == PendingSheetAction.Cancel ||
        this == PendingSheetAction.CancelPayment ||
        this == PendingSheetAction.Dismiss

private fun PendingSheetAction.icon(): DrawableResource =
    when (this) {
        PendingSheetAction.Edit -> Res.drawable.ic_edit
        PendingSheetAction.Cancel, PendingSheetAction.CancelPayment, PendingSheetAction.Dismiss -> Res.drawable.ic_block
        PendingSheetAction.SaveAnyway -> Res.drawable.ic_check
        PendingSheetAction.SendAnyway -> Res.drawable.ic_send
    }

private fun PendingSheetAction.label(): StringResource =
    when (this) {
        PendingSheetAction.Edit -> Res.string.ledger_action_edit
        PendingSheetAction.Cancel -> Res.string.ledger_action_cancel_payment
        PendingSheetAction.SaveAnyway -> Res.string.pending_action_save_anyway
        PendingSheetAction.SendAnyway -> Res.string.pending_action_send_anyway
        PendingSheetAction.CancelPayment, PendingSheetAction.Dismiss -> Res.string.pending_action_discard
    }
