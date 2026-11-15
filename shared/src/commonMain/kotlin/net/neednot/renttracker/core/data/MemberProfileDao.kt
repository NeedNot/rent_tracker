package net.neednot.renttracker.core.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberProfileDao {
    @Query(
        "SELECT member_profiles.* FROM member_profiles " +
            "JOIN memberships ON memberships.accountId = member_profiles.accountId " +
            "WHERE memberships.groupId = :groupId",
    )
    fun observeByGroupId(groupId: String): Flow<List<MemberProfileEntity>>

    @Upsert
    suspend fun upsert(profiles: List<MemberProfileEntity>)
}
