package net.neednot.renttracker.feature.tenants.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.neednot.renttracker.core.designsystem.components.DividedRows
import net.neednot.renttracker.core.designsystem.components.RtDivider
import net.neednot.renttracker.core.designsystem.components.RtListItem
import net.neednot.renttracker.core.designsystem.components.SectionCard
import net.neednot.renttracker.core.designsystem.components.Tag
import net.neednot.renttracker.core.designsystem.components.TagKind
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.tenants.presentation.uistate.RentTermRowUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.tenant_change_rent
import net.neednot.renttracker.resources.tenant_rent_term_current
import net.neednot.renttracker.resources.tenant_rent_terms_heading
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun RentTermsCard(
    rentTerms: List<RentTermRowUiState>,
    isEditable: Boolean,
    onChangeRentClicked: () -> Unit,
    onRentTermClicked: (rentTermId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(
        heading = stringResource(Res.string.tenant_rent_terms_heading),
        modifier = modifier,
        actionLabel = if (isEditable) stringResource(Res.string.tenant_change_rent) else null,
        onActionClicked = onChangeRentClicked,
    ) {
        RtDivider()
        DividedRows(items = rentTerms) { term ->
            RtListItem(
                text = term.summary.asString(),
                supportingText = term.period.asString(),
                onClick = if (isEditable) ({ onRentTermClicked(term.rentTermId) }) else null,
                trailing =
                    if (term.isCurrent) {
                        { Tag(text = stringResource(Res.string.tenant_rent_term_current), kind = TagKind.Current) }
                    } else {
                        null
                    },
            )
        }
    }
}
