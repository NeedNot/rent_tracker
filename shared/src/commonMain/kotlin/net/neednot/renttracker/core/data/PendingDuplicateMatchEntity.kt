package net.neednot.renttracker.core.data

import androidx.room.Entity

@Entity(tableName = "pending_duplicate_matches", primaryKeys = ["clientId", "matchId"])
data class PendingDuplicateMatchEntity(
    val clientId: String,
    val matchId: String,
    val kind: DuplicateMatchKind,
    val date: String,
    val amountCents: Long,
    val method: PaymentMethod?,
    val byName: String,
    val byAccountId: String,
)
