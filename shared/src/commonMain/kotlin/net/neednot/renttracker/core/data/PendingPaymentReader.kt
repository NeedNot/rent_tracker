package net.neednot.renttracker.core.data

import kotlinx.coroutines.flow.Flow

class PendingPaymentReader(
    database: RentTrackerDatabase,
) {
    private val dao = database.pendingPaymentDao()

    fun observeForTenant(tenantId: String): Flow<List<PendingPaymentEntity>> = dao.observeByTenantId(tenantId)

    fun observeRejectedWithoutCachedTenant(): Flow<List<PendingPaymentEntity>> =
        dao.observeWithStatusWithoutCachedTenant(PendingPaymentStatus.Rejected)

    fun observe(clientId: String): Flow<PendingPaymentEntity?> = dao.observeByClientId(clientId)

    fun observeDuplicateMatches(clientId: String): Flow<List<PendingDuplicateMatchEntity>> =
        dao.observeDuplicateMatches(clientId)

    fun observeUnsyncedCount(): Flow<Int> = dao.observeCount()
}
