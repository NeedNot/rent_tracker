package net.neednot.renttracker.feature.home.domain

import kotlinx.coroutines.flow.Flow
import net.neednot.renttracker.core.sync.Freshness

interface HomeOfflineRepository {
    fun observeFreshness(): Flow<Freshness>

    fun observeRejectedPayments(): Flow<List<RejectedPayment>>

    suspend fun dismissRejectedPayment(clientId: String)
}
