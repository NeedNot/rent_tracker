package net.neednot.renttracker.core.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MembershipDao {
    @Query("SELECT * FROM memberships WHERE groupId = :groupId")
    fun observeByGroupId(groupId: String): Flow<MembershipEntity?>

    @Upsert
    suspend fun upsert(memberships: List<MembershipEntity>)

    @Query("DELETE FROM memberships WHERE groupId IN (:groupIds) AND id NOT IN (:keptMembershipIds)")
    suspend fun deleteInGroupsExcept(
        groupIds: List<String>,
        keptMembershipIds: List<String>,
    )
}
