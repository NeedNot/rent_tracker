package net.neednot.renttracker.feature.tenants.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.neednot.renttracker.core.designsystem.components.DividedRows
import net.neednot.renttracker.core.designsystem.components.EmptyState
import net.neednot.renttracker.core.designsystem.components.RtDivider
import net.neednot.renttracker.core.designsystem.components.SectionCard
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerItemUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ic_receipt_long
import net.neednot.renttracker.resources.ledger_empty_body
import net.neednot.renttracker.resources.ledger_empty_title
import net.neednot.renttracker.resources.ledger_heading
import net.neednot.renttracker.resources.ledger_view_full
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LedgerPreviewCard(
    tenantName: String,
    items: List<LedgerItemUiState>,
    actions: LedgerItemActions,
    onViewFullLedgerClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(
        heading = stringResource(Res.string.ledger_heading),
        modifier = modifier,
        footerLabel = if (items.isEmpty()) null else stringResource(Res.string.ledger_view_full),
        onFooterClicked = onViewFullLedgerClicked,
    ) {
        if (items.isEmpty()) {
            EmptyState(
                icon = Res.drawable.ic_receipt_long,
                title = stringResource(Res.string.ledger_empty_title),
                text = stringResource(Res.string.ledger_empty_body, tenantName),
            )
        } else {
            RtDivider()
            DividedRows(items = items) { item -> LedgerItem(item = item, actions = actions) }
        }
    }
}
