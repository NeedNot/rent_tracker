package net.neednot.renttracker.core.data

import androidx.room.Entity

@Entity(tableName = "ledger_rows", primaryKeys = ["tenantId", "key"])
data class LedgerRowEntity(
    val tenantId: String,
    val key: String,
    val kind: LedgerRowKind,
    val entryId: String?,
    val month: String?,
    val date: String,
    val amountCents: Long,
    val position: Long,
    val balanceOwedCents: Long?,
    val balanceCreditCents: Long?,
    val mayNotApply: Boolean,
)
