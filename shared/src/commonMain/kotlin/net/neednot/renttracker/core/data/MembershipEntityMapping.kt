package net.neednot.renttracker.core.data

import net.neednot.renttracker.model.shared.Membership
import net.neednot.renttracker.model.tenants.Account

fun Account.toEntity(): AccountNameEntity = AccountNameEntity(accountId = accountId, displayName = displayName)

fun Membership.toEntity(): MembershipEntity =
    MembershipEntity(
        id = id,
        groupId = groupId,
        accountId = accountId,
        role = MembershipRole.valueOf(role.name),
        permissions =
            PermissionSet(
                tenants = accessLevelOf(tenants),
                rentTerms = accessLevelOf(rentTerms),
                payments = accessLevelOf(payments),
                debts = accessLevelOf(debts),
                members = accessLevelOf(members),
                settings = settingsAccessLevelOf(settings),
                editOthers = editOthers,
            ),
        creationTime = creationTime.toLong(),
        version = version,
    )
