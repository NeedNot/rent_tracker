package net.neednot.renttracker.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
@Suppress("TooManyFunctions")
interface PendingPaymentDao {
    @Insert
    suspend fun insert(payment: PendingPaymentEntity)

    @Update
    suspend fun update(payment: PendingPaymentEntity)

    @Query("SELECT COALESCE(MAX(sequence), 0) + 1 FROM pending_payments")
    suspend fun nextSequence(): Long

    @Query("SELECT * FROM pending_payments ORDER BY sequence")
    fun observeAll(): Flow<List<PendingPaymentEntity>>

    @Query("SELECT * FROM pending_payments WHERE tenantId = :tenantId ORDER BY sequence")
    fun observeByTenantId(tenantId: String): Flow<List<PendingPaymentEntity>>

    @Query("SELECT * FROM pending_payments WHERE clientId = :clientId")
    fun observeByClientId(clientId: String): Flow<PendingPaymentEntity?>

    @Query("SELECT * FROM pending_payments WHERE clientId = :clientId")
    suspend fun byClientId(clientId: String): PendingPaymentEntity?

    @Query("SELECT * FROM pending_payments WHERE status = :status ORDER BY sequence")
    suspend fun withStatus(status: PendingPaymentStatus): List<PendingPaymentEntity>

    @Query("SELECT COUNT(*) FROM pending_payments WHERE status = :status")
    fun observeCountWithStatus(status: PendingPaymentStatus): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_payments")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM pending_duplicate_matches WHERE clientId = :clientId ORDER BY date, matchId")
    fun observeDuplicateMatches(clientId: String): Flow<List<PendingDuplicateMatchEntity>>

    @Insert
    suspend fun insertDuplicateMatches(matches: List<PendingDuplicateMatchEntity>)

    @Query("DELETE FROM pending_duplicate_matches WHERE clientId = :clientId")
    suspend fun deleteDuplicateMatches(clientId: String)

    @Query("DELETE FROM pending_payments WHERE clientId = :clientId")
    suspend fun deleteByClientId(clientId: String)

    @Query("DELETE FROM pending_payments")
    suspend fun deleteAll()

    @Query("DELETE FROM pending_duplicate_matches")
    suspend fun deleteAllDuplicateMatches()

    @Query(
        """
        SELECT t.groupId, t.name AS tenantName,
            s.rentOwedCents + s.lateFeesOwedCents + s.otherOwedCents - s.creditCents AS balanceCents
        FROM tenants t LEFT JOIN tenant_summaries s ON s.tenantId = t.id
        WHERE t.id = :tenantId
        """,
    )
    suspend fun cachedTenant(tenantId: String): CachedPendingTenant?

    @Query("SELECT * FROM tenants WHERE id = :tenantId")
    suspend fun tenant(tenantId: String): TenantEntity?

    @Query(
        """
        SELECT 'tenant:' || version FROM tenants WHERE id = :tenantId
        UNION ALL SELECT 'rentTerm:' || id || ':' || version FROM rent_terms WHERE tenantId = :tenantId
        UNION ALL SELECT 'payment:' || id || ':' || version FROM payments WHERE tenantId = :tenantId
        UNION ALL SELECT 'debt:' || id || ':' || version FROM debts WHERE tenantId = :tenantId
        ORDER BY 1
        """,
    )
    suspend fun tenantFingerprintParts(tenantId: String): List<String>
}
