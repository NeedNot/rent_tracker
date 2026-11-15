package net.neednot.renttracker.feature.tenants.presentation.components

import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingRowUiState

private val DateColumnWidth = 56.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PendingLedgerRow(
    row: PendingRowUiState,
    onClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val greyed = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button, onClick = onClicked)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = row.date,
            modifier = Modifier.width(DateColumnWidth),
            color = greyed,
            style = MaterialTheme.typography.bodyMedium,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = row.title.asString(), color = greyed, style = MaterialTheme.typography.bodyLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                PendingChipLabel(row.chip)
                Text(text = row.detail.asString(), color = greyed, style = MaterialTheme.typography.bodySmall)
            }
        }
        Text(
            text = row.amount.asString(),
            textAlign = TextAlign.End,
            color = greyed,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
