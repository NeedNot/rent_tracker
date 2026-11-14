package net.neednot.renttracker.core.data

import androidx.room.Entity

@Entity(tableName = "calendar_months", primaryKeys = ["tenantId", "month"])
data class CalendarMonthEntity(
    val tenantId: String,
    val month: String,
    val state: CalendarMonthState,
    val rentCents: Long,
    val coveredCents: Long,
    val owedCents: Long,
    val hasUnpaidLateFee: Boolean,
    val dueDate: String?,
    val graceEnd: String?,
    val wasLate: Boolean,
    val paidByGraceEndCents: Long?,
    val lateFeeCents: Long?,
    val lateFeeUnpaidCents: Long?,
    val suggestedLateFeeCents: Long?,
)
