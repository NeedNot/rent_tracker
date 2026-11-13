package net.neednot.renttracker.feature.tenants.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantDetailsUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.tenant_details_contact
import net.neednot.renttracker.resources.tenant_details_deposit
import net.neednot.renttracker.resources.tenant_details_deposit_note
import net.neednot.renttracker.resources.tenant_details_edit
import net.neednot.renttracker.resources.tenant_details_email
import net.neednot.renttracker.resources.tenant_details_grace_period
import net.neednot.renttracker.resources.tenant_details_heading
import net.neednot.renttracker.resources.tenant_details_last_day
import net.neednot.renttracker.resources.tenant_details_late_fee
import net.neednot.renttracker.resources.tenant_details_moved_in
import net.neednot.renttracker.resources.tenant_details_not_set
import net.neednot.renttracker.resources.tenant_details_notes
import net.neednot.renttracker.resources.tenant_details_phone
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TenantDetailsCard(
    details: TenantDetailsUiState,
    isEditable: Boolean,
    onEditClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(
        heading = stringResource(Res.string.tenant_details_heading),
        modifier = modifier,
        actionLabel = if (isEditable) stringResource(Res.string.tenant_details_edit) else null,
        onActionClicked = onEditClicked,
    ) {
        Detail(label = stringResource(Res.string.tenant_details_contact), value = details.contactName)
        Detail(label = stringResource(Res.string.tenant_details_phone), value = details.phone)
        Detail(label = stringResource(Res.string.tenant_details_email), value = details.email)
        Detail(label = stringResource(Res.string.tenant_details_moved_in), value = details.movedIn)
        Detail(label = stringResource(Res.string.tenant_details_last_day), value = details.lastDayInUnit)
        Detail(
            label = stringResource(Res.string.tenant_details_deposit),
            value = details.securityDeposit,
            note = stringResource(Res.string.tenant_details_deposit_note).takeIf { details.securityDeposit != null },
        )
        Detail(label = stringResource(Res.string.tenant_details_grace_period), value = details.gracePeriod.asString())
        Detail(label = stringResource(Res.string.tenant_details_late_fee), value = details.lateFee.asString())
        Detail(label = stringResource(Res.string.tenant_details_notes), value = details.notes)
    }
}

@Composable
private fun Detail(
    label: String,
    value: String?,
    note: String? = null,
) {
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {}
                .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = value ?: stringResource(Res.string.tenant_details_not_set),
            color =
                if (value ==
                    null
                ) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            style = MaterialTheme.typography.bodyMedium,
        )
        note?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
