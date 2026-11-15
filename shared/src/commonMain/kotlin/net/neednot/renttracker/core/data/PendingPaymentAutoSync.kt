package net.neednot.renttracker.core.data

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

class PendingPaymentAutoSync(
    private val connectivity: Connectivity,
    database: RentTrackerDatabase,
    private val sync: PendingPaymentSync,
) {
    private val dao = database.pendingPaymentDao()

    suspend fun syncWhileOnline() {
        combine(connectivity.isOnline, dao.observeCountWithStatus(PendingPaymentStatus.Pending)) { online, waiting ->
            if (online) waiting else 0
        }.distinctUntilChanged()
            .filter { waiting -> waiting > 0 }
            .conflate()
            .collect { sync.syncAll() }
    }
}
