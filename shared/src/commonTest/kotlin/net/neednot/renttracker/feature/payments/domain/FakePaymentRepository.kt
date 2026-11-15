package net.neednot.renttracker.feature.payments.domain

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate
import net.neednot.renttracker.core.sync.Freshness
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced

class FakePaymentRepository : PaymentRepository {
    val entry = MutableStateFlow<Synced<PaymentEntryOverview?, PaymentError>>(Synced(null, SyncStatus.Syncing))

    val freshness = MutableStateFlow<Freshness>(Freshness.Online)
    val pendingPayment = MutableStateFlow<PendingPayment?>(null)

    var logResult: Result<LoggedPayment, PaymentError> = Ok(LoggedPayment.Saved("p9"))
    var editResult: Result<Unit, PaymentError> = Ok(Unit)
    var editPendingResult: Result<Unit, PaymentError> = Ok(Unit)
    var previewResult: Result<List<MonthStateChange>, PaymentError> = Ok(emptyList())
    var previewGate: CompletableDeferred<Unit>? = null
    var saveGate: CompletableDeferred<Unit>? = null

    val calls = mutableListOf<PaymentRepositoryCall>()

    override fun observeEntry(
        tenantId: String,
        paymentId: String?,
    ): Flow<Synced<PaymentEntryOverview?, PaymentError>> = entry

    override fun observeFreshness(tenantId: String): Flow<Freshness> = freshness

    override fun observePendingPayment(clientId: String): Flow<PendingPayment?> = pendingPayment

    override suspend fun logPayment(
        clientId: String,
        tenantId: String,
        draft: PaymentDraft,
        lateFee: LateFeeDraft?,
        duplicatePolicy: DuplicatePolicy,
    ): Result<LoggedPayment, PaymentError> {
        calls += PaymentRepositoryCall.Log(clientId, tenantId, draft, lateFee, duplicatePolicy)
        saveGate?.await()
        return logResult
    }

    override suspend fun editPayment(
        paymentId: String,
        expectedVersion: Long,
        draft: PaymentDraft,
    ): Result<Unit, PaymentError> {
        calls += PaymentRepositoryCall.Edit(paymentId, expectedVersion, draft)
        saveGate?.await()
        return editResult
    }

    override suspend fun editPendingPayment(
        clientId: String,
        draft: PaymentDraft,
    ): Result<Unit, PaymentError> {
        calls += PaymentRepositoryCall.EditPending(clientId, draft)
        return editPendingResult
    }

    override suspend fun previewPaymentChange(
        tenantId: String,
        paymentId: String,
        amountCents: Long,
        dateReceived: LocalDate,
    ): Result<List<MonthStateChange>, PaymentError> {
        calls += PaymentRepositoryCall.Preview(tenantId, paymentId, amountCents, dateReceived)
        previewGate?.await()
        return previewResult
    }
}
