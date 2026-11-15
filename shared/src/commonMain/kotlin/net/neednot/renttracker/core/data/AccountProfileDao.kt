package net.neednot.renttracker.core.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountProfileDao {
    @Query("SELECT * FROM account_profile LIMIT 1")
    fun observe(): Flow<AccountProfileEntity?>

    @Upsert
    suspend fun upsert(profile: AccountProfileEntity)

    @Query("DELETE FROM account_profile")
    suspend fun deleteAll()
}
