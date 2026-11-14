package net.neednot.renttracker.feature.group.presentation.viewmodel

import net.neednot.renttracker.core.designsystem.components.TenantChipState
import net.neednot.renttracker.core.designsystem.format.formatMoney
import net.neednot.renttracker.core.designsystem.format.formatMonthDay
import net.neednot.renttracker.core.designsystem.format.owesText
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.group.domain.GroupDetails
import net.neednot.renttracker.feature.group.domain.GroupError
import net.neednot.renttracker.feature.group.domain.GroupTenant
import net.neednot.renttracker.feature.group.domain.GroupTenantState
import net.neednot.renttracker.feature.group.domain.TenantBalance
import net.neednot.renttracker.feature.group.presentation.uistate.GroupUiState
import net.neednot.renttracker.feature.group.presentation.uistate.TenantRowUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.balance_paid_up
import net.neednot.renttracker.resources.group_archived
import net.neednot.renttracker.resources.group_archived_owed
import net.neednot.renttracker.resources.group_tenant_contact_and_due
import net.neednot.renttracker.resources.group_tenant_count_late
import net.neednot.renttracker.resources.group_tenant_count_none_late
import net.neednot.renttracker.resources.group_tenant_due

internal fun Synced<GroupDetails?, GroupError>.toGroupUiState(): GroupUiState {
    val syncError = (status as? SyncStatus.Failed)?.error?.text()
    val group = value
    return when {
        group != null && group.activeTenants.isNotEmpty() -> group.toContent(syncError)
        syncError != null -> GroupUiState.Error(syncError)
        group == null || status == SyncStatus.Syncing -> GroupUiState.Loading
        else -> GroupUiState.Empty(groupName = group.name, archived = group.archivedText())
    }
}

private fun GroupDetails.toContent(syncError: UiText?): GroupUiState.Content =
    GroupUiState.Content(
        groupName = name,
        balance = if (totalOwedCents == 0L) UiText.Resource(Res.string.balance_paid_up) else owesText(totalOwedCents),
        tenantCounts = tenantCountsText(),
        archived = archivedText(),
        tenants = activeTenants.map { it.toRowUiState() },
        syncError = syncError,
    )

private fun GroupDetails.tenantCountsText(): UiText {
    val tenantCount = activeTenants.size
    if (lateTenantCount == 0) {
        return UiText.Plural(Res.plurals.group_tenant_count_none_late, tenantCount, listOf(tenantCount))
    }
    return UiText.Plural(Res.plurals.group_tenant_count_late, tenantCount, listOf(tenantCount, lateTenantCount))
}

private fun GroupDetails.archivedText(): UiText? {
    val archivedCount = archivedTenants.size
    return when {
        archivedCount == 0 -> null
        archivedOwedCents == 0L -> UiText.Resource(Res.string.group_archived, listOf(archivedCount))
        else -> UiText.Resource(Res.string.group_archived_owed, listOf(archivedCount, formatMoney(archivedOwedCents)))
    }
}

private fun GroupTenant.toRowUiState(): TenantRowUiState =
    TenantRowUiState(
        tenantId = tenantId,
        name = name,
        detail = detailText(),
        balance = if (balance == TenantBalance.PaidUp) null else balance.text(),
        chipState = state.toChipState(),
    )

private fun GroupTenant.detailText(): UiText? {
    val dueDate = nextDueDate?.let(::formatMonthDay)
    return when {
        contactName != null && dueDate != null -> {
            UiText.Resource(Res.string.group_tenant_contact_and_due, listOf(contactName, dueDate))
        }

        dueDate != null -> {
            UiText.Resource(Res.string.group_tenant_due, listOf(dueDate))
        }

        contactName != null -> {
            UiText.Plain(contactName)
        }

        else -> {
            null
        }
    }
}

private fun GroupTenantState.toChipState(): TenantChipState =
    when (this) {
        GroupTenantState.Late -> TenantChipState.Late
        GroupTenantState.Owes -> TenantChipState.Owes
        GroupTenantState.PaidUp -> TenantChipState.PaidUp
    }
