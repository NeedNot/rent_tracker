package net.neednot.renttracker.feature.tenants.data

import net.neednot.renttracker.core.data.MembershipEntity
import net.neednot.renttracker.core.data.MembershipRole
import net.neednot.renttracker.feature.tenants.domain.CallerPermissions

internal fun MembershipEntity.toCallerPermissions(): CallerPermissions =
    CallerPermissions(
        accountId = accountId,
        isOwner = role == MembershipRole.Owner,
        payments = payments,
        debts = debts,
        editOthers = editOthers,
    )
