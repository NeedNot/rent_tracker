package net.neednot.renttracker.core.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupSummaryDao {
    @Query("SELECT * FROM group_summaries")
    fun observeAll(): Flow<List<GroupSummaryEntity>>

    @Query("SELECT * FROM group_summaries WHERE groupId = :groupId")
    fun observeByGroupId(groupId: String): Flow<GroupSummaryEntity?>

    @Upsert
    suspend fun upsert(groupSummaries: List<GroupSummaryEntity>)

    @Query("DELETE FROM group_summaries WHERE groupId NOT IN (:keptGroupIds)")
    suspend fun deleteAllExcept(keptGroupIds: List<String>)
}
