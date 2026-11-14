package net.neednot.renttracker.core.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TenantDao {
    @Query("SELECT * FROM tenants WHERE groupId = :groupId AND status = :status ORDER BY name COLLATE NOCASE")
    fun observeByGroupAndStatus(
        groupId: String,
        status: TenantStatus,
    ): Flow<List<TenantEntity>>

    @Query("SELECT * FROM tenants WHERE id = :tenantId")
    fun observeById(tenantId: String): Flow<TenantEntity?>

    @Upsert
    suspend fun upsert(tenants: List<TenantEntity>)

    @Query("DELETE FROM tenants WHERE groupId = :groupId AND id NOT IN (:keptTenantIds)")
    suspend fun deleteInGroupExcept(
        groupId: String,
        keptTenantIds: List<String>,
    )
}
