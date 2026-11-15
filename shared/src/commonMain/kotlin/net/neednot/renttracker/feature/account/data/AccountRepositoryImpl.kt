package net.neednot.renttracker.feature.account.data

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import net.neednot.renttracker.core.auth.AuthSession
import net.neednot.renttracker.core.backend.Backend
import net.neednot.renttracker.core.backend.BackendError
import net.neednot.renttracker.core.backend.ConvexJson
import net.neednot.renttracker.core.data.AccountProfileSync
import net.neednot.renttracker.core.data.PendingPaymentQueue
import net.neednot.renttracker.core.data.PendingPaymentReader
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.telemetry.Severity
import net.neednot.renttracker.core.telemetry.Telemetry
import net.neednot.renttracker.feature.account.domain.AccountError
import net.neednot.renttracker.feature.account.domain.AccountProfile
import net.neednot.renttracker.feature.account.domain.AccountRepository
import net.neednot.renttracker.model.accounts.AccountsSetDisplayNameArgs
import kotlin.time.Duration.Companion.seconds

class AccountRepositoryImpl(
    private val backend: Backend,
    private val authSession: AuthSession,
    private val accountProfileSync: AccountProfileSync,
    private val pendingPaymentQueue: PendingPaymentQueue,
    private val pendingPaymentReader: PendingPaymentReader,
    private val telemetry: Telemetry,
) : AccountRepository {
    override fun observeProfile(): Flow<Synced<AccountProfile?, AccountError>> =
        accountProfileSync.observe().map { synced ->
            Synced(
                value = synced.value?.toAccountProfile(),
                status = synced.status.toAccountStatus(),
            )
        }

    override suspend fun editName(displayName: String): Result<Unit, AccountError> =
        mutate(
            name = SET_DISPLAY_NAME,
            args =
                ConvexJson.encodeArgs(
                    AccountsSetDisplayNameArgs.serializer(),
                    AccountsSetDisplayNameArgs(displayName = displayName.trim()),
                ),
        )

    override suspend fun requestDeletion(): Result<Unit, AccountError> =
        mutate(name = REQUEST_DELETION, args = JsonObject(emptyMap()))

    override fun observeUnsyncedPaymentCount(): Flow<Int> = pendingPaymentReader.observeUnsyncedCount()

    override suspend fun signOut(): Result<Unit, AccountError> =
        authSession
            .signOut()
            .onOk {
                accountProfileSync.clear()
                pendingPaymentQueue.clear()
            }.mapError { it.toAccountError() }
            .reportingUnexpected()

    private suspend fun mutate(
        name: String,
        args: JsonObject,
    ): Result<Unit, AccountError> {
        val answer =
            withTimeoutOrNull(BACKEND_TIMEOUT) {
                backend.mutation(name, args, JsonElement.serializer())
            } ?: return Err(AccountError.Offline)
        return answer.map { }.mapError { it.toAccountError() }.reportingUnexpected()
    }

    private fun <T> Result<T, AccountError>.reportingUnexpected(): Result<T, AccountError> =
        onErr { error ->
            if (error is AccountError.Unexpected) telemetry.log(TAG, error.message, Severity.Error)
        }

    private fun SyncStatus<BackendError>.toAccountStatus(): SyncStatus<AccountError> =
        when (this) {
            SyncStatus.Syncing -> SyncStatus.Syncing
            SyncStatus.Synced -> SyncStatus.Synced
            is SyncStatus.Failed -> SyncStatus.Failed(error.toAccountError())
        }

    private companion object {
        const val TAG = "AccountRepository"
        const val SET_DISPLAY_NAME = "accounts:setDisplayName"
        const val REQUEST_DELETION = "accounts:requestDeletion"

        // The Convex client queues calls while it has no connection instead of failing them.
        val BACKEND_TIMEOUT = 20.seconds
    }
}
