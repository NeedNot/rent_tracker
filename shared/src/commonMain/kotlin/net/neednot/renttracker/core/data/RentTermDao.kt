package net.neednot.renttracker.core.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RentTermDao {
    @Query("SELECT * FROM rent_terms WHERE tenantId = :tenantId ORDER BY startMonth, creationTime")
    fun observeByTenantId(tenantId: String): Flow<List<RentTermEntity>>

    @Upsert
    suspend fun upsert(rentTerms: List<RentTermEntity>)

    @Query("DELETE FROM rent_terms WHERE tenantId = :tenantId AND id NOT IN (:keptRentTermIds)")
    suspend fun deleteOfTenantExcept(
        tenantId: String,
        keptRentTermIds: List<String>,
    )
}
