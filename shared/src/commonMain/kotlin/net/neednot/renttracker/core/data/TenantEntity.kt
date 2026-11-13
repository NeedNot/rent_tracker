package net.neednot.renttracker.core.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "tenants", indices = [Index("groupId")])
data class TenantEntity(
    @PrimaryKey val id: String,
    val creationTime: Long,
    val groupId: String,
    val name: String,
    val status: TenantStatus,
    val moveIn: String,
    val moveOut: String?,
    val depositCents: Long?,
    val contactName: String?,
    val contactPhone: String?,
    val contactEmail: String?,
    val notes: String?,
    val graceDaysOverride: Long?,
    val lateFeeFlatOverrideCents: Long?,
    val lastPaymentMethod: PaymentMethod?,
    val lastEditedAt: Long?,
    val lastEditedBy: String?,
    val version: Long,
)
