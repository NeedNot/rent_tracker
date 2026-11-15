package net.neednot.renttracker.feature.home.domain

import kotlinx.coroutines.flow.MutableStateFlow
import net.neednot.renttracker.core.sync.Freshness

class FakeHomeOfflineRepository : HomeOfflineRepository {
    val freshness = MutableStateFlow<Freshness>(Freshness.Online)
    val rejectedPayments = MutableStateFlow<List<RejectedPayment>>(emptyList())
    val dismissedClientIds = mutableListOf<String>()

    override fun observeFreshness() = freshness

    override fun observeRejectedPayments() = rejectedPayments

    override suspend fun dismissRejectedPayment(clientId: String) {
        dismissedClientIds += clientId
    }
}
