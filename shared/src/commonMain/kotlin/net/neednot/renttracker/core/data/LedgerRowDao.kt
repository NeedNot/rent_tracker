package net.neednot.renttracker.core.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerRowDao {
    @Query("SELECT * FROM ledger_rows WHERE tenantId = :tenantId ORDER BY position DESC")
    fun observeByTenantId(tenantId: String): Flow<List<LedgerRowEntity>>

    @Upsert
    suspend fun upsert(ledgerRows: List<LedgerRowEntity>)

    @Query("DELETE FROM ledger_rows WHERE tenantId = :tenantId AND `key` NOT IN (:keptKeys)")
    suspend fun deleteOfTenantExcept(
        tenantId: String,
        keptKeys: List<String>,
    )
}
