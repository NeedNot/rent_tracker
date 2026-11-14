package net.neednot.renttracker.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "account_names")
data class AccountNameEntity(
    @PrimaryKey val accountId: String,
    val displayName: String,
)
