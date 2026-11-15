package net.neednot.renttracker.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "signed_in_account")
data class SignedInAccountEntity(
    @PrimaryKey val accountId: String,
)
