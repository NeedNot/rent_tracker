package net.neednot.renttracker.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "member_profiles")
data class MemberProfileEntity(
    @PrimaryKey val accountId: String,
    val displayName: String,
    val email: String?,
)
