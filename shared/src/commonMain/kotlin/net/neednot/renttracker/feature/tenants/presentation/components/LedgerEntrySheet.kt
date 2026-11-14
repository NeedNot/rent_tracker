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
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Visibility
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
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerFieldUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerSheetAction
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerSheetUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ledger_action_cancel_charge
import net.neednot.renttracker.resources.ledger_action_cancel_payment
import net.neednot.renttracker.resources.ledger_action_edit
import net.neednot.renttracker.resources.ledger_action_mark_bounced
import net.neednot.renttracker.resources.ledger_action_restore_clear_bounce
import net.neednot.renttracker.resources.ledger_restore
import net.neednot.renttracker.resources.ledger_view_only
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LedgerEntrySheet(
    sheet: LedgerSheetUiState,
    isWorking: Boolean,
    onActionClicked: (LedgerSheetAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = sheet.title.asString(), style = MaterialTheme.typography.titleLarge)
            sheet.chips.forEach { chip -> EntryChipLabel(chip) }
        }
        Text(text = sheet.amount, style = MaterialTheme.typography.headlineMedium)
        sheet.fields.forEach { field -> SheetField(field) }
        if (sheet.actions.isEmpty()) {
            ViewOnlyRow()
        } else {
            sheet.actions.forEach { action ->
                SheetActionButton(action = action, enabled = !isWorking, onClicked = { onActionClicked(action) })
            }
        }
    }
}

@Composable
private fun SheetField(field: LedgerFieldUiState) {
    Column {
        Text(
            text = field.label.asString(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
        Text(text = field.value.asString(), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SheetActionButton(
    action: LedgerSheetAction,
    enabled: Boolean,
    onClicked: () -> Unit,
) {
    val color =
        if (action.isDestructive()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    TextButton(
        onClick = onClicked,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = action.icon(),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = color,
            )
            Text(text = stringResource(action.label()), color = color, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ViewOnlyRow() {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Visibility,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(Res.string.ledger_view_only),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

private fun LedgerSheetAction.isDestructive(): Boolean =
    this == LedgerSheetAction.CancelPayment || this == LedgerSheetAction.CancelCharge

private fun LedgerSheetAction.icon(): ImageVector =
    when (this) {
        LedgerSheetAction.Edit -> Icons.Outlined.Edit
        LedgerSheetAction.MarkBounced -> Icons.Outlined.ErrorOutline
        LedgerSheetAction.CancelPayment, LedgerSheetAction.CancelCharge -> Icons.Outlined.Block
        LedgerSheetAction.Restore, LedgerSheetAction.RestoreClearBounce -> Icons.Outlined.Restore
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
