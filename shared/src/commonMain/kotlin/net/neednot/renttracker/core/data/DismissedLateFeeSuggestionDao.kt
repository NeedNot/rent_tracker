package net.neednot.renttracker.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DismissedLateFeeSuggestionDao {
    @Query("SELECT forMonth FROM dismissed_late_fee_suggestions WHERE tenantId = :tenantId")
    fun observeDismissedMonths(tenantId: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(suggestion: DismissedLateFeeSuggestionEntity)
}
