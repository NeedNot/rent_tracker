package net.neednot.renttracker.core.data

import net.neednot.renttracker.model.accounts.AccountsMeResult

fun AccountsMeResult.toEntity(): AccountProfileEntity =
    AccountProfileEntity(
        accountId = accountId,
        displayName = displayName,
        email = email,
        deletionRequestedAt = deletionRequestedAt,
    )
