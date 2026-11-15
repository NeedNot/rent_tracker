package net.neednot.renttracker.core.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CacheTimeDao {
    @Upsert
    suspend fun upsert(cacheTime: CacheTimeEntity)

    @Query("SELECT MIN(syncedAt) FROM cache_times WHERE scope IN (:scopes)")
    fun observeOldest(scopes: List<String>): Flow<Long?>
}
