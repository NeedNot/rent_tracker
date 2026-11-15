package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import net.neednot.renttracker.core.data.PaymentOrder
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.tenants.domain.MoveTarget
import net.neednot.renttracker.feature.tenants.domain.TenantOverview
import net.neednot.renttracker.feature.tenants.presentation.uistate.MoveConfirmationUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.MoveTargetUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.home_tenant_count
import net.neednot.renttracker.resources.move_confirm_gain_access
import net.neednot.renttracker.resources.move_confirm_history
import net.neednot.renttracker.resources.move_confirm_lose_access
import net.neednot.renttracker.resources.move_confirm_settings_by_date
import net.neednot.renttracker.resources.move_confirm_settings_rent_first
import net.neednot.renttracker.resources.move_confirm_title

internal fun MoveTarget.toUiState(isSelected: Boolean): MoveTargetUiState =
    MoveTargetUiState(
        groupId = groupId,
        name = name,
        tenantCount = UiText.Plural(Res.plurals.home_tenant_count, tenantCount, listOf(tenantCount)),
        isSelected = isSelected,
    )

internal fun TenantOverview.moveConfirmation(target: MoveTarget): MoveConfirmationUiState {
    val settings =
        when (target.paymentOrder) {
            PaymentOrder.RentFirst -> Res.string.move_confirm_settings_rent_first
            PaymentOrder.ByDate -> Res.string.move_confirm_settings_by_date
        }
    return MoveConfirmationUiState(
        title = UiText.Resource(Res.string.move_confirm_title, listOf(tenant.details.name, target.name)),
        consequences =
            listOf(
                UiText.Resource(Res.string.move_confirm_lose_access, listOf(group.name)),
                UiText.Resource(Res.string.move_confirm_gain_access, listOf(target.name)),
                UiText.Resource(settings, listOf(target.name, target.graceDays)),
                UiText.Resource(Res.string.move_confirm_history),
            ),
    )
}
