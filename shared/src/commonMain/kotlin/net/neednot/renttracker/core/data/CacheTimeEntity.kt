package net.neednot.renttracker.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cache_times")
data class CacheTimeEntity(
    @PrimaryKey val scope: String,
    val syncedAt: Long,
)
