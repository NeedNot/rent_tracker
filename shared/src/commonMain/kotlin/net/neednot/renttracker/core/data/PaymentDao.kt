package net.neednot.renttracker.core.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE tenantId = :tenantId ORDER BY dateReceived DESC, creationTime DESC")
    fun observeByTenantId(tenantId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE id = :paymentId")
    fun observeById(paymentId: String): Flow<PaymentEntity?>

    @Upsert
    suspend fun upsert(payments: List<PaymentEntity>)

    @Query("DELETE FROM payments WHERE tenantId = :tenantId AND id NOT IN (:keptPaymentIds)")
    suspend fun deleteOfTenantExcept(
        tenantId: String,
        keptPaymentIds: List<String>,
    )
}
