package net.neednot.renttracker.core.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TenantSummaryDao {
    @Query(
        """
        SELECT * FROM tenant_summaries
        WHERE tenantId IN (SELECT id FROM tenants WHERE groupId = :groupId AND status = :status)
        """,
    )
    fun observeByGroupAndStatus(
        groupId: String,
        status: TenantStatus,
    ): Flow<List<TenantSummaryEntity>>

    @Query("SELECT * FROM tenant_summaries WHERE tenantId = :tenantId")
    fun observeByTenantId(tenantId: String): Flow<TenantSummaryEntity?>

    @Upsert
    suspend fun upsert(tenantSummaries: List<TenantSummaryEntity>)

    @Query(
        """
        DELETE FROM tenant_summaries
        WHERE tenantId IN (SELECT id FROM tenants WHERE groupId = :groupId)
        AND tenantId NOT IN (:keptTenantIds)
        """,
    )
    suspend fun deleteInGroupExcept(
        groupId: String,
        keptTenantIds: List<String>,
    )
}
