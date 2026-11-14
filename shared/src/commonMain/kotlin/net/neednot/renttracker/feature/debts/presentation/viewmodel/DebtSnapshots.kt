package net.neednot.renttracker.feature.debts.presentation.viewmodel

import kotlinx.coroutines.flow.first
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.feature.debts.domain.ChargeEntryOverview
import net.neednot.renttracker.feature.debts.domain.DebtError
import net.neednot.renttracker.feature.debts.domain.DebtRepository

internal suspend fun DebtRepository.cachedOrFirstSyncedEntry(
    tenantId: String,
    debtId: String?,
): Synced<ChargeEntryOverview?, DebtError> =
    observeEntry(tenantId, debtId).first { snapshot ->
        snapshot.isComplete(debtId) || snapshot.status != SyncStatus.Syncing
    }

internal suspend fun DebtRepository.entryFromServer(
    tenantId: String,
    debtId: String?,
): Synced<ChargeEntryOverview?, DebtError> = observeEntry(tenantId, debtId).first { it.status != SyncStatus.Syncing }

internal fun Synced<ChargeEntryOverview?, DebtError>.loadError(): DebtError =
    (status as? SyncStatus.Failed)?.error ?: DebtError.NotFound

private fun Synced<ChargeEntryOverview?, DebtError>.isComplete(debtId: String?): Boolean =
    value != null && (debtId == null || value.charge != null)
