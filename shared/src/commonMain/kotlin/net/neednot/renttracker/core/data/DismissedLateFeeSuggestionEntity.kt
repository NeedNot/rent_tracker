package net.neednot.renttracker.core.data

import androidx.room.Entity

@Entity(tableName = "dismissed_late_fee_suggestions", primaryKeys = ["tenantId", "forMonth"])
data class DismissedLateFeeSuggestionEntity(
    val tenantId: String,
    val forMonth: String,
)
