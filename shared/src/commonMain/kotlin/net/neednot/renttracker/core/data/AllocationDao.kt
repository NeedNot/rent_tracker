package net.neednot.renttracker.core.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AllocationDao {
    @Query("SELECT * FROM allocations WHERE tenantId = :tenantId ORDER BY `key`")
    fun observeByTenantId(tenantId: String): Flow<List<AllocationEntity>>

    @Upsert
    suspend fun upsert(allocations: List<AllocationEntity>)

    @Query("DELETE FROM allocations WHERE tenantId = :tenantId AND `key` NOT IN (:keptKeys)")
    suspend fun deleteOfTenantExcept(
        tenantId: String,
        keptKeys: List<String>,
    )
}
