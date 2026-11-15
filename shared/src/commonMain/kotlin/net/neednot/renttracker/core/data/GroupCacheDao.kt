package net.neednot.renttracker.core.data

import androidx.room.Dao
import androidx.room.Query

@Dao
@Suppress("TooManyFunctions")
interface GroupCacheDao {
    @Query("SELECT name FROM `groups` WHERE id = :groupId")
    suspend fun groupName(groupId: String): String?

    @Query("SELECT groupId FROM tenants WHERE id = :tenantId")
    suspend fun groupIdOfTenant(tenantId: String): String?

    @Query(
        """
        DELETE FROM cache_times
        WHERE scope IN (SELECT 'tenant:' || id FROM tenants WHERE groupId = :groupId)
        OR EXISTS (
            SELECT 1 FROM tenants
            WHERE tenants.groupId = :groupId AND cache_times.scope LIKE 'calendar:' || tenants.id || ':%'
        )
        """,
    )
    suspend fun deleteTenantCacheTimes(groupId: String)

    @Query("DELETE FROM cache_times WHERE scope IN (:scopes)")
    suspend fun deleteGroupCacheTimes(vararg scopes: String)

    @Query("DELETE FROM rent_terms WHERE tenantId IN (SELECT id FROM tenants WHERE groupId = :groupId)")
    suspend fun deleteRentTerms(groupId: String)

    @Query("DELETE FROM payments WHERE tenantId IN (SELECT id FROM tenants WHERE groupId = :groupId)")
    suspend fun deletePayments(groupId: String)

    @Query("DELETE FROM debts WHERE tenantId IN (SELECT id FROM tenants WHERE groupId = :groupId)")
    suspend fun deleteDebts(groupId: String)

    @Query("DELETE FROM ledger_rows WHERE tenantId IN (SELECT id FROM tenants WHERE groupId = :groupId)")
    suspend fun deleteLedgerRows(groupId: String)

    @Query("DELETE FROM allocations WHERE tenantId IN (SELECT id FROM tenants WHERE groupId = :groupId)")
    suspend fun deleteAllocations(groupId: String)

    @Query("DELETE FROM calendar_months WHERE tenantId IN (SELECT id FROM tenants WHERE groupId = :groupId)")
    suspend fun deleteCalendarMonths(groupId: String)

    @Query("DELETE FROM tenant_summaries WHERE tenantId IN (SELECT id FROM tenants WHERE groupId = :groupId)")
    suspend fun deleteTenantSummaries(groupId: String)

    @Query("DELETE FROM tenants WHERE groupId = :groupId")
    suspend fun deleteTenants(groupId: String)

    @Query("DELETE FROM memberships WHERE groupId = :groupId")
    suspend fun deleteMemberships(groupId: String)

    @Query("DELETE FROM invites WHERE groupId = :groupId")
    suspend fun deleteInvites(groupId: String)

    @Query("DELETE FROM group_summaries WHERE groupId = :groupId")
    suspend fun deleteGroupSummary(groupId: String)

    @Query("DELETE FROM `groups` WHERE id = :groupId")
    suspend fun deleteGroup(groupId: String)
}
