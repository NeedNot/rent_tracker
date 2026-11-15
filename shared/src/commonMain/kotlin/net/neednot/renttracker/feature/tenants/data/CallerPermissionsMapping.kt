package net.neednot.renttracker.feature.tenants.data

import net.neednot.renttracker.core.access.toCallerAccess
import net.neednot.renttracker.core.data.GroupEntity
import net.neednot.renttracker.core.data.MembershipEntity
import net.neednot.renttracker.core.data.MembershipRole
import net.neednot.renttracker.feature.tenants.domain.CallerPermissions

internal fun MembershipEntity.toCallerPermissions(group: GroupEntity): CallerPermissions =
    CallerPermissions(
        accountId = accountId,
        isOwner = role == MembershipRole.Owner,
        payments = permissions.payments,
        debts = permissions.debts,
        editOthers = permissions.editOthers,
        canMoveTenants = toCallerAccess(groupArchived = group.archivedAt != null).canMoveTenants,
    )
