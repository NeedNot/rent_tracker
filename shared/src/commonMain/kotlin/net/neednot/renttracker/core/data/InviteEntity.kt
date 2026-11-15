package net.neednot.renttracker.core.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "invites", indices = [Index("groupId")])
data class InviteEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val creationTime: Long,
    val email: String,
    @Embedded val permissions: PermissionSet,
    val invitedBy: String,
    val expiresAt: Long,
)
