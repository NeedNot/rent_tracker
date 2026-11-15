package net.neednot.renttracker.core.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MembershipDao {
    @Query(
        "SELECT memberships.* FROM memberships " +
            "JOIN signed_in_account ON signed_in_account.accountId = memberships.accountId " +
            "WHERE memberships.groupId = :groupId",
    )
    fun observeCaller(groupId: String): Flow<MembershipEntity?>

    @Query(
        "SELECT * FROM memberships WHERE groupId = :groupId " +
            "ORDER BY CASE role WHEN 'Owner' THEN 0 ELSE 1 END, creationTime",
    )
    fun observeByGroupId(groupId: String): Flow<List<MembershipEntity>>

    @Upsert
    suspend fun upsert(memberships: List<MembershipEntity>)

    @Query("DELETE FROM memberships WHERE groupId = :groupId AND id NOT IN (:keptMembershipIds)")
    suspend fun deleteInGroupExcept(
        groupId: String,
        keptMembershipIds: List<String>,
    )

    @Query("DELETE FROM memberships WHERE groupId = :groupId AND accountId = :accountId AND id != :keptMembershipId")
    suspend fun deleteOtherMembershipsOfAccount(
        groupId: String,
        accountId: String,
        keptMembershipId: String,
    )
}
