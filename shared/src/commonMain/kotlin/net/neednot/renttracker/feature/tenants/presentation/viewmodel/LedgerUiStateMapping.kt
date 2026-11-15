package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import kotlinx.datetime.TimeZone
import net.neednot.renttracker.core.designsystem.format.formatDate
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.offlineBannerText
import net.neednot.renttracker.feature.tenants.domain.TenantError
import net.neednot.renttracker.feature.tenants.domain.TenantOverview
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ledger_empty_first_rent
import net.neednot.renttracker.resources.ledger_empty_no_date
import net.neednot.renttracker.resources.pending_ledger_note
import net.neednot.renttracker.resources.tenant_error_not_found

internal fun Synced<TenantOverview?, TenantError>.toLedgerUiState(
    offline: OfflineSources,
    timeZone: TimeZone,
): LedgerUiState {
    val syncError = (status as? SyncStatus.Failed)?.error?.text()
    val overview = value
    return when {
        overview != null -> overview.toLedgerContent(syncError, offline, timeZone)
        syncError != null -> LedgerUiState.Error(syncError)
        status == SyncStatus.Syncing -> LedgerUiState.Loading
        else -> LedgerUiState.Error(UiText.Resource(Res.string.tenant_error_not_found))
    }
}

private fun TenantOverview.toLedgerContent(
    syncError: UiText?,
    offline: OfflineSources,
    timeZone: TimeZone,
): LedgerUiState.Content =
    LedgerUiState.Content(
        tenantName = tenant.details.name,
        groupName = group.name,
        sections = ledgerSections(ledger, offline.pendingPayments, offline.freshness, timeZone),
        emptyMessage = if (ledger.isEmpty() && offline.pendingPayments.isEmpty()) emptyMessage() else null,
        canLogPayment = !tenant.isArchived && permissions?.canLogPayments == true,
        offlineBanner = offline.freshness.offlineBannerText(),
        pendingNote = UiText.Resource(Res.string.pending_ledger_note).takeIf { offline.pendingPayments.isNotEmpty() },
        syncError = syncError,
    )

private fun TenantOverview.emptyMessage(): UiText =
    balance?.nextDueDate?.let { UiText.Resource(Res.string.ledger_empty_first_rent, listOf(formatDate(it))) }
        ?: UiText.Resource(Res.string.ledger_empty_no_date)
