package net.neednot.renttracker.feature.home.presentation.viewmodel

import net.neednot.renttracker.core.designsystem.format.formatMoney
import net.neednot.renttracker.core.designsystem.format.owesText
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.home.domain.GroupOverview
import net.neednot.renttracker.feature.home.domain.HomeError
import net.neednot.renttracker.feature.home.presentation.uistate.GroupCardUiState
import net.neednot.renttracker.feature.home.presentation.uistate.HomeUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.balance_paid_up
import net.neednot.renttracker.resources.home_archived_owed
import net.neednot.renttracker.resources.home_late_count
import net.neednot.renttracker.resources.home_tenant_count
import net.neednot.renttracker.resources.home_tenant_count_none_late

internal fun Synced<List<GroupOverview>, HomeError>.toHomeUiState(): HomeUiState {
    val syncError = (status as? SyncStatus.Failed)?.error?.text()
    return when {
        value.isNotEmpty() -> HomeUiState.Content(groups = value.map { it.toCardUiState() }, syncError = syncError)
        syncError != null -> HomeUiState.Error(syncError)
        status == SyncStatus.Syncing -> HomeUiState.Loading
        else -> HomeUiState.Empty
    }
}

private fun GroupOverview.toCardUiState(): GroupCardUiState {
    val isPaidUp = totalOwedCents == 0L
    val isAnyoneLate = lateTenantCount > 0
    return GroupCardUiState(
        groupId = groupId,
        name = name,
        balance = if (isPaidUp) UiText.Resource(Res.string.balance_paid_up) else owesText(totalOwedCents),
        isPaidUp = isPaidUp,
        tenantCount =
            UiText.Plural(
                resource = if (isAnyoneLate) Res.plurals.home_tenant_count else Res.plurals.home_tenant_count_none_late,
                quantity = activeTenantCount,
                args = listOf(activeTenantCount),
            ),
        lateCount = if (isAnyoneLate) UiText.Resource(Res.string.home_late_count, listOf(lateTenantCount)) else null,
        archivedOwed =
            if (archivedOwedCents > 0) {
                UiText.Resource(Res.string.home_archived_owed, listOf(formatMoney(archivedOwedCents)))
            } else {
                null
            },
    )
}
