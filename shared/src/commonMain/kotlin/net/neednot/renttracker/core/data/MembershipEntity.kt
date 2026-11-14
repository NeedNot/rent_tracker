package net.neednot.renttracker.core.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "memberships", indices = [Index("groupId")])
data class MembershipEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val accountId: String,
    val role: MembershipRole,
    val tenants: AccessLevel,
    val rentTerms: AccessLevel,
    val payments: AccessLevel,
    val debts: AccessLevel,
    val members: AccessLevel,
    val settings: SettingsAccessLevel,
    val editOthers: Boolean,
)
