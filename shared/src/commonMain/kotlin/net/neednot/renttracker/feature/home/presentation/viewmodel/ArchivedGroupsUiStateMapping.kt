package net.neednot.renttracker.feature.home.presentation.viewmodel

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.neednot.renttracker.core.designsystem.format.formatDate
import net.neednot.renttracker.core.designsystem.format.owesText
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.home.domain.ArchivedGroup
import net.neednot.renttracker.feature.home.domain.HomeError
import net.neednot.renttracker.feature.home.presentation.uistate.ArchivedGroupCardUiState
import net.neednot.renttracker.feature.home.presentation.uistate.ArchivedGroupsUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.archived_groups_summary
import net.neednot.renttracker.resources.balance_paid_up

internal fun Synced<List<ArchivedGroup>, HomeError>.toArchivedGroupsUiState(
    timeZone: TimeZone,
): ArchivedGroupsUiState {
    val syncError = (status as? SyncStatus.Failed)?.error?.text()
    return when {
        value.isNotEmpty() -> ArchivedGroupsUiState.Content(value.map { it.toCardUiState(timeZone) }, syncError)
        syncError != null -> ArchivedGroupsUiState.Error(syncError)
        status == SyncStatus.Syncing -> ArchivedGroupsUiState.Loading
        else -> ArchivedGroupsUiState.Empty
    }
}

private fun ArchivedGroup.toCardUiState(timeZone: TimeZone): ArchivedGroupCardUiState =
    ArchivedGroupCardUiState(
        groupId = groupId,
        name = name,
        balance = if (totalOwedCents == 0L) UiText.Resource(Res.string.balance_paid_up) else owesText(totalOwedCents),
        summary =
            UiText.Plural(
                resource = Res.plurals.archived_groups_summary,
                quantity = tenantCount,
                args = listOf(tenantCount, formatDate(archivedAt.toLocalDateTime(timeZone).date)),
            ),
    )
