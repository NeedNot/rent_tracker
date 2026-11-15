package net.neednot.renttracker.feature.account.domain

import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.Flow
import net.neednot.renttracker.core.sync.Synced

interface AccountRepository {
    fun observeProfile(): Flow<Synced<AccountProfile?, AccountError>>

    suspend fun editName(displayName: String): Result<Unit, AccountError>

    suspend fun requestDeletion(): Result<Unit, AccountError>

    fun observeUnsyncedPaymentCount(): Flow<Int>

    suspend fun signOut(): Result<Unit, AccountError>
}
