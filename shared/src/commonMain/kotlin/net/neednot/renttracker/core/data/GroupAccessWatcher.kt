package net.neednot.renttracker.core.data

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.onErr
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.neednot.renttracker.core.backend.BackendError

class GroupAccessWatcher(
    database: RentTrackerDatabase,
    private val screenResultStore: ScreenResultStore,
) {
    private val groupCacheDao = database.groupCacheDao()
    private val reporting = Mutex()
    private val pendingLosses =
        Channel<LostGroupAccess>(capacity = BUFFER_CAPACITY, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val losses: Flow<LostGroupAccess> = pendingLosses.receiveAsFlow()

    fun <T> watchGroup(
        groupId: String,
        results: Flow<Result<T, BackendError>>,
    ): Flow<Result<T, BackendError>> =
        results.onEach { result ->
            result.onErr { error -> error.groupAccessLoss()?.let { reason -> report(groupId, reason) } }
        }

    fun <T> watchTenant(
        tenantId: String,
        results: Flow<Result<T, BackendError>>,
    ): Flow<Result<T, BackendError>> =
        results.onEach { result ->
            result.onErr { error ->
                if (error.isPermissionDenied()) reportForTenant(tenantId, AccessLossReason.Removed)
            }
        }

    suspend fun report(
        groupId: String,
        reason: AccessLossReason,
    ) = reporting.withLock {
        val groupName = groupCacheDao.groupName(groupId) ?: return@withLock
        screenResultStore.clearGroup(groupId)
        pendingLosses.send(LostGroupAccess(groupId, groupName, reason))
    }

    suspend fun reportForTenant(
        tenantId: String,
        reason: AccessLossReason,
    ) {
        val groupId = groupCacheDao.groupIdOfTenant(tenantId) ?: return
        report(groupId, reason)
    }

    private fun BackendError.groupAccessLoss(): AccessLossReason? =
        when {
            isPermissionDenied() -> AccessLossReason.Removed
            this is BackendError.Convex && code == NOT_FOUND -> AccessLossReason.GroupGone
            else -> null
        }

    private fun BackendError.isPermissionDenied(): Boolean = this is BackendError.Convex && code == PERMISSION_DENIED

    private companion object {
        const val BUFFER_CAPACITY = 4
        const val PERMISSION_DENIED = "PermissionDenied"
        const val NOT_FOUND = "NotFound"
    }
}
