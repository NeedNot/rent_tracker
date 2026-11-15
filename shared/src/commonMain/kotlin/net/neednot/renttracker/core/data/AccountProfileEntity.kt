package net.neednot.renttracker.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "account_profile")
data class AccountProfileEntity(
    @PrimaryKey val accountId: String,
    val displayName: String,
    val email: String,
    val deletionRequestedAt: Long?,
)
