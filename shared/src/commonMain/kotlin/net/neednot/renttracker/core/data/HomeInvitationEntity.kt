package net.neednot.renttracker.core.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "home_invitations")
data class HomeInvitationEntity(
    @PrimaryKey val inviteId: String,
    val groupId: String,
    val groupName: String,
    val invitedByName: String,
    @Embedded val permissions: PermissionSet,
    val expiresAt: Long,
)
