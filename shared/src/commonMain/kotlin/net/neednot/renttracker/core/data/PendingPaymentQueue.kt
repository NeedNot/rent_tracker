package net.neednot.renttracker.core.data

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.onOk
import kotlinx.coroutines.flow.Flow

class PendingPaymentQueue(
    private val database: RentTrackerDatabase,
    private val scheduler: PendingPaymentSyncScheduler,
) {
    private val dao = database.pendingPaymentDao()

    fun observeForTenant(tenantId: String): Flow<List<PendingPaymentEntity>> = dao.observeByTenantId(tenantId)

    fun observe(clientId: String): Flow<PendingPaymentEntity?> = dao.observeByClientId(clientId)

    fun observeDuplicateMatches(clientId: String): Flow<List<PendingDuplicateMatchEntity>> =
        dao.observeDuplicateMatches(clientId)

    fun observeUnsyncedCount(): Flow<Int> = dao.observeCount()

    suspend fun enqueue(
        clientId: String,
        tenantId: String,
        details: PendingPaymentDetails,
    ): Result<Unit, PendingQueueError> =
        database
            .inWriteTransaction {
                val tenant =
                    dao.cachedTenant(tenantId) ?: return@inWriteTransaction Err(PendingQueueError.TenantNotCached)
                dao.insert(
                    PendingPaymentEntity(
                        clientId = clientId,
                        sequence = dao.nextSequence(),
                        groupId = tenant.groupId,
                        tenantId = tenantId,
                        tenantName = tenant.tenantName,
                        amountCents = details.amountCents,
                        dateReceived = details.dateReceived,
                        method = details.method,
                        note = details.note,
                        status = PendingPaymentStatus.Pending,
                        confirmationReason = null,
                        rejectionReason = null,
                        rejectionMessage = null,
                        duplicateConfirmed = false,
                        tenantFingerprint = dao.tenantFingerprint(tenantId),
                        balanceCentsWhenLogged = tenant.balanceCents,
                    ),
                )
                Ok(Unit)
            }.onOk { scheduler.scheduleSync() }

    suspend fun edit(
        clientId: String,
        details: PendingPaymentDetails,
    ): Result<Unit, PendingQueueError> =
        changeUnrejected(clientId) { payment ->
            payment.copy(
                amountCents = details.amountCents,
                dateReceived = details.dateReceived,
                method = details.method,
                note = details.note,
                duplicateConfirmed = false,
            )
        }

    suspend fun confirm(clientId: String): Result<Unit, PendingQueueError> =
        changeUnrejected(clientId) { payment ->
            payment.copy(duplicateConfirmed = payment.duplicateConfirmed || payment.isPossibleDuplicate())
        }

    suspend fun discard(clientId: String) {
        database.inWriteTransaction {
            dao.deleteDuplicateMatches(clientId)
            dao.deleteByClientId(clientId)
        }
    }

    suspend fun clear() {
        database.inWriteTransaction {
            dao.deleteAllDuplicateMatches()
            dao.deleteAll()
        }
    }

    private suspend fun changeUnrejected(
        clientId: String,
        change: (PendingPaymentEntity) -> PendingPaymentEntity,
    ): Result<Unit, PendingQueueError> =
        database
            .inWriteTransaction {
                val payment = dao.byClientId(clientId) ?: return@inWriteTransaction Err(PendingQueueError.NotFound)
                if (payment.status == PendingPaymentStatus.Rejected) {
                    return@inWriteTransaction Err(PendingQueueError.NotEditable)
                }
                dao.deleteDuplicateMatches(clientId)
                val fingerprint = dao.tenantFingerprint(payment.tenantId)
                dao.update(change(payment).backToPending(fingerprint, dao.cachedTenant(payment.tenantId)))
                Ok(Unit)
            }.onOk { scheduler.scheduleSync() }
}

private fun PendingPaymentEntity.isPossibleDuplicate(): Boolean =
    confirmationReason == PendingConfirmationReason.PossibleDuplicate

private fun PendingPaymentEntity.backToPending(
    currentFingerprint: String,
    currentTenant: CachedPendingTenant?,
): PendingPaymentEntity =
    copy(
        status = PendingPaymentStatus.Pending,
        confirmationReason = null,
        tenantFingerprint = currentFingerprint,
        balanceCentsWhenLogged = currentTenant?.balanceCents ?: balanceCentsWhenLogged,
    )
