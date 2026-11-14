package net.neednot.renttracker.feature.tenants.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerRowUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ledger_may_not_apply
import net.neednot.renttracker.resources.ledger_restore
import org.jetbrains.compose.resources.stringResource

private val DateColumnWidth = 56.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LedgerRow(
    row: LedgerRowUiState,
    onClicked: () -> Unit,
    onRestoreClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strike = if (row.isStruckThrough) TextDecoration.LineThrough else TextDecoration.None
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(enabled = row.isTappable, role = Role.Button, onClick = onClicked)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = row.date,
            modifier = Modifier.width(DateColumnWidth),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = row.title.asString(), textDecoration = strike, style = MaterialTheme.typography.bodyLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                row.chips.forEach { chip -> EntryChipLabel(chip) }
            }
            row.details.forEach { detail ->
                Text(
                    text = detail.asString(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (row.mayNotApply) {
                Text(
                    text = stringResource(Res.string.ledger_may_not_apply),
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (row.canRestore) {
                TextButton(onClick = onRestoreClicked, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(Res.string.ledger_restore))
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = row.amount.asString(),
                textAlign = TextAlign.End,
                textDecoration = strike,
                style = MaterialTheme.typography.bodyLarge,
            )
            row.balance?.let { balance ->
                Text(
                    text = balance.asString(),
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
