package net.neednot.renttracker.core.access

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import net.neednot.renttracker.core.data.MembershipEntity
import net.neednot.renttracker.core.data.RentTrackerDatabase

fun RentTrackerDatabase.observeCallerAccess(groupId: String): Flow<CallerAccess?> =
    combine(membershipDao().observeCaller(groupId), groupDao().observeById(groupId)) { caller, group ->
        if (caller == null || group == null) return@combine null
        caller.toCallerAccess(groupArchived = group.archivedAt != null)
    }

fun MembershipEntity.toCallerAccess(groupArchived: Boolean): CallerAccess =
    CallerAccess(
        membershipId = id,
        accountId = accountId,
        role = role,
        permissions = permissions.effectiveFor(role),
        groupArchived = groupArchived,
    )
