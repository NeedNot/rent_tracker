package net.neednot.renttracker.feature.tenants.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.designsystem.components.SectionCard
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerItemUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ledger_heading
import net.neednot.renttracker.resources.ledger_preview_empty
import net.neednot.renttracker.resources.ledger_view_full
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LedgerPreviewCard(
    items: List<LedgerItemUiState>,
    actions: LedgerItemActions,
    onViewFullLedgerClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(heading = stringResource(Res.string.ledger_heading), modifier = modifier) {
        if (items.isEmpty()) {
            Text(
                text = stringResource(Res.string.ledger_preview_empty),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        items.forEachIndexed { index, item ->
            if (index > 0) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            LedgerItem(item = item, actions = actions)
        }
        TextButton(
            onClick = onViewFullLedgerClicked,
            modifier = Modifier.padding(horizontal = 8.dp).heightIn(min = 48.dp),
        ) {
            Text(stringResource(Res.string.ledger_view_full))
        }
    }
}
