package net.neednot.renttracker.core.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts WHERE tenantId = :tenantId ORDER BY date DESC, creationTime DESC")
    fun observeByTenantId(tenantId: String): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE id = :debtId")
    fun observeById(debtId: String): Flow<DebtEntity?>

    @Upsert
    suspend fun upsert(debts: List<DebtEntity>)

    @Query("DELETE FROM debts WHERE tenantId = :tenantId AND id NOT IN (:keptDebtIds)")
    suspend fun deleteOfTenantExcept(
        tenantId: String,
        keptDebtIds: List<String>,
    )
}
