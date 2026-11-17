package net.neednot.renttracker.feature.tenants.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.neednot.renttracker.core.designsystem.components.DetailRow
import net.neednot.renttracker.core.designsystem.components.SectionCard
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantDetailsUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.tenant_details_contact
import net.neednot.renttracker.resources.tenant_details_deposit
import net.neednot.renttracker.resources.tenant_details_deposit_value
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
    val notSet = stringResource(Res.string.tenant_details_not_set)
    SectionCard(
        heading = stringResource(Res.string.tenant_details_heading),
        modifier = modifier,
        actionLabel = if (isEditable) stringResource(Res.string.tenant_details_edit) else null,
        onActionClicked = onEditClicked,
    ) {
        DetailRow(stringResource(Res.string.tenant_details_contact), details.contactName ?: notSet)
        DetailRow(stringResource(Res.string.tenant_details_phone), details.phone ?: notSet)
        DetailRow(stringResource(Res.string.tenant_details_email), details.email ?: notSet)
        DetailRow(stringResource(Res.string.tenant_details_moved_in), details.movedIn)
        DetailRow(stringResource(Res.string.tenant_details_last_day), details.lastDayInUnit ?: notSet)
        DetailRow(
            label = stringResource(Res.string.tenant_details_deposit),
            value =
                details.securityDeposit?.let { stringResource(Res.string.tenant_details_deposit_value, it) } ?: notSet,
        )
        DetailRow(stringResource(Res.string.tenant_details_grace_period), details.gracePeriod.asString())
        DetailRow(stringResource(Res.string.tenant_details_late_fee), details.lateFee.asString())
        DetailRow(stringResource(Res.string.tenant_details_notes), details.notes ?: notSet)
    }
}
