package net.neednot.renttracker.core.data

import com.github.michaelbull.result.fold
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import net.neednot.renttracker.core.backend.Backend
import net.neednot.renttracker.core.backend.BackendError
import net.neednot.renttracker.core.backend.ConvexJson
import net.neednot.renttracker.core.telemetry.Severity
import net.neednot.renttracker.core.telemetry.Telemetry
import net.neednot.renttracker.model.payments.PaymentsLogArgs
import net.neednot.renttracker.model.payments.PaymentsLogMethod
import net.neednot.renttracker.model.payments.PaymentsLogResult
import kotlin.time.Duration.Companion.seconds

class PendingPaymentSender(
    private val backend: Backend,
    private val database: RentTrackerDatabase,
    private val telemetry: Telemetry,
) {
    private val dao = database.pendingPaymentDao()

    suspend fun send(payment: PendingPaymentEntity): PendingSendOutcome {
        val args = ConvexJson.encodeArgs(PaymentsLogArgs.serializer(), payment.toLogArgs())
        val answer =
            withTimeoutOrNull(BACKEND_TIMEOUT) {
                backend.mutation(LOG_PAYMENT, args, PaymentsLogResult.serializer())
            } ?: return PendingSendOutcome.RetryLater
        return answer.fold(
            success = {
                database.inWriteTransaction {
                    dao.deleteDuplicateMatches(payment.clientId)
                    dao.deleteByClientId(payment.clientId)
                }
                PendingSendOutcome.Accepted
            },
            failure = { error -> error.toSendOutcome() },
        )
    }

    private fun BackendError.toSendOutcome(): PendingSendOutcome =
        when {
            isRetryable() -> PendingSendOutcome.RetryLater.also { telemetry.log(TAG, toString(), Severity.Warning) }
            this is BackendError.Convex && code == NEEDS_CONFIRMATION -> possibleDuplicate()
            this is BackendError.Convex -> PendingSendOutcome.Rejected(rejectionReason(), message)
            else -> PendingSendOutcome.RetryLater
        }

    private fun BackendError.Convex.possibleDuplicate(): PendingSendOutcome {
        val matches = details?.matchesOrNull()
        if (matches == null) {
            telemetry.log(TAG, "NeedsConfirmation without matches: $message", Severity.Error)
            return PendingSendOutcome.Rejected(PendingRejectionReason.Other, message)
        }
        return PendingSendOutcome.PossibleDuplicate(matches)
    }

    private fun BackendError.Convex.rejectionReason(): PendingRejectionReason =
        when (code) {
            PERMISSION_DENIED, NOT_FOUND -> PendingRejectionReason.NoPermission
            TENANT_ARCHIVED -> PendingRejectionReason.TenantArchived
            TENANT_MOVED -> PendingRejectionReason.TenantMoved
            else -> PendingRejectionReason.Other
        }

    private fun JsonElement.matchesOrNull(): List<DuplicateMatchDetails>? =
        try {
            ConvexJson.decode(NeedsConfirmationDetails.serializer(), this).matches
        } catch (_: SerializationException) {
            null
        }

    private companion object {
        const val TAG = "PendingPaymentSender"
        const val LOG_PAYMENT = "payments:log"
        const val NEEDS_CONFIRMATION = "NeedsConfirmation"
        const val PERMISSION_DENIED = "PermissionDenied"
        const val NOT_FOUND = "NotFound"
        const val TENANT_ARCHIVED = "TenantArchived"
        const val TENANT_MOVED = "TenantMoved"

        // The Convex client queues calls while it has no connection instead of failing them.
        val BACKEND_TIMEOUT = 20.seconds
    }
}

private fun PendingPaymentEntity.toLogArgs(): PaymentsLogArgs =
    PaymentsLogArgs(
        tenantId = tenantId,
        expectedGroupId = groupId,
        clientId = clientId,
        amountCents = amountCents,
        dateReceived = dateReceived,
        method = method?.let { PaymentsLogMethod.valueOf(it.name) },
        note = note,
        confirmed = if (duplicateConfirmed) true else null,
    )
