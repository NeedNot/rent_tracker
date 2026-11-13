package net.neednot.renttracker.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val id: String,
    val creationTime: Long,
    val name: String,
    val timeZone: String,
    val paymentOrder: PaymentOrder,
    val graceDays: Long,
    val lateFeeFlatCents: Long?,
    val ownerId: String,
    val archivedAt: Long?,
    val lastEditedAt: Long?,
    val lastEditedBy: String?,
    val version: Long,
)
