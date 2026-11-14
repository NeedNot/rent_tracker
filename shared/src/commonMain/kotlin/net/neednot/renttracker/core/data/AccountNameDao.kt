package net.neednot.renttracker.core.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountNameDao {
    @Query("SELECT * FROM account_names")
    fun observeAll(): Flow<List<AccountNameEntity>>

    @Query("SELECT * FROM account_names WHERE accountId IN (:accountIds)")
    fun observeByIds(accountIds: List<String>): Flow<List<AccountNameEntity>>

    @Upsert
    suspend fun upsert(accountNames: List<AccountNameEntity>)
}
