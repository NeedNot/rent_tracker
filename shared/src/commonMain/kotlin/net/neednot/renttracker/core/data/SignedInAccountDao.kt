package net.neednot.renttracker.core.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface SignedInAccountDao {
    @Upsert
    suspend fun upsert(account: SignedInAccountEntity)

    @Query("DELETE FROM signed_in_account WHERE accountId != :accountId")
    suspend fun deleteAllExcept(accountId: String)
}
