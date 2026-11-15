package net.neednot.renttracker.feature.home.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.neednot.renttracker.core.data.CacheScope
import net.neednot.renttracker.core.data.FreshnessSource
import net.neednot.renttracker.core.data.PendingPaymentQueue
import net.neednot.renttracker.core.data.PendingPaymentReader
import net.neednot.renttracker.core.sync.Freshness
import net.neednot.renttracker.feature.home.domain.HomeOfflineRepository
import net.neednot.renttracker.feature.home.domain.RejectedPayment

class HomeOfflineRepositoryImpl(
    private val freshnessSource: FreshnessSource,
    private val pendingPaymentReader: PendingPaymentReader,
    private val pendingPaymentQueue: PendingPaymentQueue,
) : HomeOfflineRepository {
    override fun observeFreshness(): Flow<Freshness> = freshnessSource.observe(listOf(CacheScope.HOME))

    override fun observeRejectedPayments(): Flow<List<RejectedPayment>> =
        pendingPaymentReader.observeRejectedWithoutCachedTenant().map { payments ->
            payments.map { it.toRejectedPayment() }
        }

    override suspend fun dismissRejectedPayment(clientId: String) {
        pendingPaymentQueue.discard(clientId)
    }
}
