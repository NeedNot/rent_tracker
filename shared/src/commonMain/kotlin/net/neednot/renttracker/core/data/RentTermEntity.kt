package net.neednot.renttracker.core.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "rent_terms", indices = [Index("tenantId")])
data class RentTermEntity(
    @PrimaryKey val id: String,
    val creationTime: Long,
    val tenantId: String,
    val startMonth: String,
    val amountCents: Long,
    val dueDay: Long,
    val deletedAt: Long?,
    val deletedBy: String?,
    val deleteReason: String?,
    val lastEditedAt: Long?,
    val lastEditedBy: String?,
    val version: Long,
)
