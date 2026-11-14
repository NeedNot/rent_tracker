package net.neednot.renttracker.core.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "debts", indices = [Index("tenantId")])
data class DebtEntity(
    @PrimaryKey val id: String,
    val creationTime: Long,
    val tenantId: String,
    val kind: DebtKind,
    val amountCents: Long,
    val date: String,
    val forMonth: String?,
    val note: String?,
    val createdBy: String,
    val deletedAt: Long?,
    val deletedBy: String?,
    val deleteReason: String?,
    val lastEditedAt: Long?,
    val lastEditedBy: String?,
    val version: Long,
)
