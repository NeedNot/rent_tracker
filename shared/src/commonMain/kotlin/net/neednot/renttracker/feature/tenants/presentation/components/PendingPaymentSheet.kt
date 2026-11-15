package net.neednot.renttracker.feature.tenants.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingSheetAction
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingSheetUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ledger_action_cancel_payment
import net.neednot.renttracker.resources.ledger_action_edit
import net.neednot.renttracker.resources.pending_action_cancel
import net.neednot.renttracker.resources.pending_action_dismiss
import net.neednot.renttracker.resources.pending_action_save_anyway
import net.neednot.renttracker.resources.pending_action_send_anyway
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PendingPaymentSheet(
    sheet: PendingSheetUiState,
    isWorking: Boolean,
    onActionClicked: (PendingSheetAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = sheet.title.asString(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleSmall,
            )
            PendingChipLabel(sheet.chip)
        }
        Text(
            text = sheet.amount,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.headlineMedium,
        )
        PendingSheetBodyContent(sheet.body)
        SheetActions(sheet.actions, isWorking, onActionClicked)
    }
}

@Composable
private fun SheetActions(
    actions: List<PendingSheetAction>,
    isWorking: Boolean,
    onActionClicked: (PendingSheetAction) -> Unit,
) {
    val rowActions = actions.filter { it != PendingSheetAction.Dismiss }
    if (rowActions.isNotEmpty()) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
    rowActions.forEach { action ->
        ActionRow(action = action, enabled = !isWorking, onClicked = { onActionClicked(action) })
    }
    if (PendingSheetAction.Dismiss in actions) {
        FilledTonalButton(
            onClick = { onActionClicked(PendingSheetAction.Dismiss) },
            enabled = !isWorking,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp).heightIn(min = 48.dp),
        ) {
            Text(stringResource(Res.string.pending_action_dismiss))
        }
    }
}

@Composable
private fun ActionRow(
    action: PendingSheetAction,
    enabled: Boolean,
    onClicked: () -> Unit,
) {
    val color = if (action.isDestructive()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    TextButton(
        onClick = onClicked,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = action.icon(), contentDescription = null, modifier = Modifier.size(20.dp), tint = color)
            Text(text = stringResource(action.label()), color = color, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private fun PendingSheetAction.isDestructive(): Boolean =
    this == PendingSheetAction.Cancel || this == PendingSheetAction.CancelPayment

private fun PendingSheetAction.icon(): ImageVector =
    when (this) {
        PendingSheetAction.Edit -> Icons.Outlined.Edit
        PendingSheetAction.Cancel, PendingSheetAction.CancelPayment, PendingSheetAction.Dismiss -> Icons.Outlined.Block
        PendingSheetAction.SaveAnyway -> Icons.Outlined.CheckCircle
        PendingSheetAction.SendAnyway -> Icons.AutoMirrored.Outlined.Send
    }

private fun PendingSheetAction.label(): StringResource =
    when (this) {
        PendingSheetAction.Edit -> Res.string.ledger_action_edit
        PendingSheetAction.Cancel -> Res.string.pending_action_cancel
        PendingSheetAction.SaveAnyway -> Res.string.pending_action_save_anyway
        PendingSheetAction.SendAnyway -> Res.string.pending_action_send_anyway
        PendingSheetAction.CancelPayment -> Res.string.ledger_action_cancel_payment
        PendingSheetAction.Dismiss -> Res.string.pending_action_dismiss
    }
