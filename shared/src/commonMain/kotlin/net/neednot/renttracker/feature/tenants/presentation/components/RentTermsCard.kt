package net.neednot.renttracker.feature.tenants.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.designsystem.components.SectionCard
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.tenants.presentation.uistate.RentTermRowUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.tenant_change_rent
import net.neednot.renttracker.resources.tenant_rent_term_current
import net.neednot.renttracker.resources.tenant_rent_term_edit
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
        rentTerms.forEach { term ->
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
            RentTermRow(term = term, isEditable = isEditable, onClick = { onRentTermClicked(term.rentTermId) })
        }
    }
}

@Composable
private fun RentTermRow(
    term: RentTermRowUiState,
    isEditable: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = isEditable,
                    onClickLabel = stringResource(Res.string.tenant_rent_term_edit),
                    role = Role.Button,
                    onClick = onClick,
                ).heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = term.summary.asString(),
                color =
                    if (term.isCurrent) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = term.period.asString(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (term.isCurrent) CurrentBadge()
    }
}

@Composable
private fun CurrentBadge() {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            text = stringResource(Res.string.tenant_rent_term_current),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
