package net.neednot.renttracker.core.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PendingPaymentSync(
    private val refresher: TenantRefresher,
    private val sender: PendingPaymentSender,
    private val database: RentTrackerDatabase,
) {
    private val dao = database.pendingPaymentDao()
    private val running = Mutex()

    suspend fun syncAll(): PendingSyncReport =
        running.withLock {
            val run = SyncRun()
            for (queued in dao.withStatus(PendingPaymentStatus.Pending)) {
                if (!syncOne(queued.clientId, run)) return@withLock run.report(retryLater = true)
            }
            run.report(retryLater = false)
        }

    private suspend fun syncOne(
        clientId: String,
        run: SyncRun,
    ): Boolean {
        val payment = dao.byClientId(clientId)?.takeIf { it.status == PendingPaymentStatus.Pending } ?: return true
        val before = run.refreshed.getOrPut(payment.tenantId) { refresher.refresh(payment.tenantId) }
        return when {
            before is TenantRefresh.Unreachable -> false
            before.changedSince(payment) -> run.flag(markTenantChanged(payment))
            else -> handle(sender.send(payment), payment, before, run)
        }
    }

    private suspend fun handle(
        outcome: PendingSendOutcome,
        payment: PendingPaymentEntity,
        before: TenantRefresh,
        run: SyncRun,
    ): Boolean =
        when (outcome) {
            PendingSendOutcome.Accepted -> {
                val after = refresher.refresh(payment.tenantId)
                rebaselineSiblings(payment, before, after)
                run.refreshed[payment.tenantId] = after
                true
            }

            is PendingSendOutcome.PossibleDuplicate -> {
                run.flag(markPossibleDuplicate(payment, outcome.matches))
            }

            is PendingSendOutcome.Rejected -> {
                run.flag(markRejected(payment, outcome))
            }

            PendingSendOutcome.RetryLater -> {
                false
            }
        }

    private fun TenantRefresh.changedSince(payment: PendingPaymentEntity): Boolean =
        this is TenantRefresh.Current && isActiveIn(payment.groupId) && fingerprint != payment.tenantFingerprint

    private suspend fun rebaselineSiblings(
        sent: PendingPaymentEntity,
        before: TenantRefresh,
        after: TenantRefresh,
    ) {
        if (before !is TenantRefresh.Current || after !is TenantRefresh.Current) return
        database.inWriteTransaction {
            dao
                .withStatus(PendingPaymentStatus.Pending)
                .filter { it.tenantId == sent.tenantId && it.tenantFingerprint == before.fingerprint }
                .forEach { sibling -> dao.update(sibling.copy(tenantFingerprint = after.fingerprint)) }
        }
    }

    private suspend fun markTenantChanged(payment: PendingPaymentEntity): PendingPaymentEntity =
        payment
            .copy(
                status = PendingPaymentStatus.NeedsConfirmation,
                confirmationReason = PendingConfirmationReason.TenantChanged,
            ).also { dao.update(it) }

    private suspend fun markPossibleDuplicate(
        payment: PendingPaymentEntity,
        matches: List<DuplicateMatchDetails>,
    ): PendingPaymentEntity {
        val marked =
            payment.copy(
                status = PendingPaymentStatus.NeedsConfirmation,
                confirmationReason = PendingConfirmationReason.PossibleDuplicate,
            )
        database.inWriteTransaction {
            dao.update(marked)
            dao.deleteDuplicateMatches(payment.clientId)
            dao.insertDuplicateMatches(matches.map { it.toEntity(payment.clientId) })
        }
        return marked
    }

    private suspend fun markRejected(
        payment: PendingPaymentEntity,
        rejection: PendingSendOutcome.Rejected,
    ): PendingPaymentEntity =
        payment
            .copy(
                status = PendingPaymentStatus.Rejected,
                confirmationReason = null,
                rejectionReason = rejection.reason,
                rejectionMessage = rejection.message,
            ).also { dao.update(it) }

    private class SyncRun {
        val refreshed = mutableMapOf<String, TenantRefresh>()
        private val needsAttention = mutableListOf<PendingPaymentEntity>()

        fun flag(payment: PendingPaymentEntity): Boolean {
            needsAttention += payment
            return true
        }

        fun report(retryLater: Boolean) = PendingSyncReport(needsAttention.toList(), retryLater)
    }
}

private fun DuplicateMatchDetails.toEntity(clientId: String): PendingDuplicateMatchEntity =
    PendingDuplicateMatchEntity(
        clientId = clientId,
        matchId = id,
        kind = if (kind == DuplicateKindDetails.Payment) DuplicateMatchKind.Payment else DuplicateMatchKind.LateFee,
        date = date,
        amountCents = amountCents,
        method = method?.let { PaymentMethod.valueOf(it.name) },
        byName = byName,
        byAccountId = byAccountId,
    )
