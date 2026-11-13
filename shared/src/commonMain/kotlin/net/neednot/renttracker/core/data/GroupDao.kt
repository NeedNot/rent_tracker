package net.neednot.renttracker.core.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    @Query("SELECT * FROM `groups` ORDER BY creationTime")
    fun observeAll(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM `groups` WHERE id = :groupId")
    fun observeById(groupId: String): Flow<GroupEntity?>

    @Upsert
    suspend fun upsert(groups: List<GroupEntity>)

    @Query("DELETE FROM `groups` WHERE id NOT IN (:keptGroupIds)")
    suspend fun deleteAllExcept(keptGroupIds: List<String>)
}
