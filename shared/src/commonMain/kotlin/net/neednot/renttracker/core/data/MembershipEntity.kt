package net.neednot.renttracker.core.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "memberships", indices = [Index("groupId")])
data class MembershipEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val accountId: String,
    val role: MembershipRole,
    @Embedded val permissions: PermissionSet,
    @ColumnInfo(defaultValue = "0") val creationTime: Long,
    @ColumnInfo(defaultValue = "0") val version: Long,
)
