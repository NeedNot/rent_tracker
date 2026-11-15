package net.neednot.renttracker.core.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface HomeInvitationDao {
    @Query("SELECT * FROM home_invitations ORDER BY expiresAt")
    fun observeAll(): Flow<List<HomeInvitationEntity>>

    @Upsert
    suspend fun upsert(invitations: List<HomeInvitationEntity>)

    @Query("DELETE FROM home_invitations WHERE inviteId NOT IN (:keptInviteIds)")
    suspend fun deleteAllExcept(keptInviteIds: List<String>)
}
