package net.neednot.renttracker.core.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarMonthDao {
    @Query(
        "SELECT * FROM calendar_months WHERE tenantId = :tenantId " +
            "AND CAST(substr(month, 1, 4) AS INTEGER) = :year ORDER BY month",
    )
    fun observeYear(
        tenantId: String,
        year: Int,
    ): Flow<List<CalendarMonthEntity>>

    @Query("SELECT * FROM calendar_months WHERE tenantId = :tenantId AND month = :month")
    fun observeMonth(
        tenantId: String,
        month: String,
    ): Flow<CalendarMonthEntity?>

    @Upsert
    suspend fun upsert(calendarMonths: List<CalendarMonthEntity>)

    @Query("DELETE FROM calendar_months WHERE tenantId = :tenantId AND CAST(substr(month, 1, 4) AS INTEGER) = :year")
    suspend fun deleteYear(
        tenantId: String,
        year: Int,
    )
}
