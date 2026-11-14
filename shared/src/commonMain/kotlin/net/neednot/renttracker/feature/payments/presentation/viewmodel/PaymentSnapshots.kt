package net.neednot.renttracker.feature.payments.presentation.viewmodel

import kotlinx.coroutines.flow.first
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.feature.payments.domain.PaymentEntryOverview
import net.neednot.renttracker.feature.payments.domain.PaymentError
import net.neednot.renttracker.feature.payments.domain.PaymentRepository

internal suspend fun PaymentRepository.cachedOrFirstSyncedEntry(
    tenantId: String,
    paymentId: String?,
): Synced<PaymentEntryOverview?, PaymentError> =
    observeEntry(tenantId, paymentId).first { snapshot ->
        snapshot.isComplete(paymentId) || snapshot.status != SyncStatus.Syncing
    }

internal suspend fun PaymentRepository.entryFromServer(
    tenantId: String,
    paymentId: String?,
): Synced<PaymentEntryOverview?, PaymentError> =
    observeEntry(tenantId, paymentId).first { it.status != SyncStatus.Syncing }

internal fun Synced<PaymentEntryOverview?, PaymentError>.loadError(): PaymentError =
    (status as? SyncStatus.Failed)?.error ?: PaymentError.NotFound

private fun Synced<PaymentEntryOverview?, PaymentError>.isComplete(paymentId: String?): Boolean =
    value != null && (paymentId == null || value.payment != null)
