package net.neednot.renttracker.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingPaymentDao {
    @Insert
    suspend fun insert(payment: PendingPaymentEntity)

    @Query("SELECT * FROM pending_payments")
    fun observeAll(): Flow<List<PendingPaymentEntity>>

    @Query("DELETE FROM pending_payments WHERE clientId = :clientId")
    suspend fun deleteByClientId(clientId: String)
}
