package net.neednot.renttracker.core.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface InviteDao {
    @Query("SELECT * FROM invites WHERE groupId = :groupId ORDER BY creationTime")
    fun observeByGroupId(groupId: String): Flow<List<InviteEntity>>

    @Upsert
    suspend fun upsert(invites: List<InviteEntity>)

    @Query("DELETE FROM invites WHERE groupId = :groupId AND id NOT IN (:keptInviteIds)")
    suspend fun deleteInGroupExcept(
        groupId: String,
        keptInviteIds: List<String>,
    )
}
