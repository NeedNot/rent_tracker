package net.neednot.renttracker.feature.payments.data

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.datetime.LocalDate
import net.neednot.renttracker.core.backend.Backend
import net.neednot.renttracker.core.data.AsOfClock
import net.neednot.renttracker.core.data.RentTrackerDatabase
import net.neednot.renttracker.core.data.ScreenResultStore
import net.neednot.renttracker.core.data.replaceTenant
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.sync.syncedFlow
import net.neednot.renttracker.core.telemetry.Telemetry
import net.neednot.renttracker.feature.payments.domain.DuplicatePolicy
import net.neednot.renttracker.feature.payments.domain.LateFeeDraft
import net.neednot.renttracker.feature.payments.domain.MonthStateChange
import net.neednot.renttracker.feature.payments.domain.PaymentDraft
import net.neednot.renttracker.feature.payments.domain.PaymentEntryOverview
import net.neednot.renttracker.feature.payments.domain.PaymentError
import net.neednot.renttracker.feature.payments.domain.PaymentRepository
import net.neednot.renttracker.model.payments.PaymentsEditArgs
import net.neednot.renttracker.model.payments.PaymentsLogArgs
import net.neednot.renttracker.model.payments.PaymentsLogResult
import net.neednot.renttracker.model.payments.PaymentsPreviewArgs
import net.neednot.renttracker.model.payments.PaymentsPreviewResult
import net.neednot.renttracker.model.tenants.TenantsScreenArgs
import net.neednot.renttracker.model.tenants.TenantsScreenResult

class PaymentRepositoryImpl(
    backend: Backend,
    private val asOfClock: AsOfClock,
    private val database: RentTrackerDatabase,
    private val screenResultStore: ScreenResultStore,
    telemetry: Telemetry,
) : PaymentRepository {
    private val calls = PaymentBackendCalls(backend, telemetry)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeEntry(
        tenantId: String,
        paymentId: String?,
    ): Flow<Synced<PaymentEntryOverview?, PaymentError>> =
        syncedFlow(
            cached = database.observePaymentEntry(tenantId, paymentId, asOfClock.asOf),
            serverResults =
                asOfClock.asOf.flatMapLatest { asOf ->
                    calls.subscribe(
                        name = TENANT_SCREEN,
                        args = TenantsScreenArgs(asOf = asOf, tenantId = tenantId),
                        argsSerializer = TenantsScreenArgs.serializer(),
                        resultSerializer = TenantsScreenResult.serializer(),
                    )
                },
            store = { result -> screenResultStore.replaceTenant(tenantId, result) },
        )

    override suspend fun logPayment(
        clientId: String,
        tenantId: String,
        draft: PaymentDraft,
        lateFee: LateFeeDraft?,
        duplicatePolicy: DuplicatePolicy,
    ): Result<String, PaymentError> =
        calls
            .mutate(
                name = LOG_PAYMENT,
                args = draft.toLogArgs(clientId, tenantId, lateFee, duplicatePolicy),
                argsSerializer = PaymentsLogArgs.serializer(),
                resultSerializer = PaymentsLogResult.serializer(),
            ).map { it.paymentId }

    override suspend fun editPayment(
        paymentId: String,
        expectedVersion: Long,
        draft: PaymentDraft,
    ): Result<Unit, PaymentError> =
        calls.mutateWithoutAnswer(
            name = EDIT_PAYMENT,
            args = draft.toEditArgs(paymentId, expectedVersion),
            argsSerializer = PaymentsEditArgs.serializer(),
        )

    override suspend fun previewPaymentChange(
        tenantId: String,
        paymentId: String,
        amountCents: Long,
        dateReceived: LocalDate,
    ): Result<List<MonthStateChange>, PaymentError> =
        calls
            .queryOnce(
                name = PREVIEW_PAYMENT,
                args =
                    PaymentsPreviewArgs(
                        asOf = asOfClock.asOf.first(),
                        proposed = toProposed(paymentId, amountCents, dateReceived),
                        tenantId = tenantId,
                    ),
                argsSerializer = PaymentsPreviewArgs.serializer(),
                resultSerializer = PaymentsPreviewResult.serializer(),
            ).map { it.toMonthStateChanges() }

    private companion object {
        const val TENANT_SCREEN = "tenants:screen"
        const val LOG_PAYMENT = "payments:log"
        const val EDIT_PAYMENT = "payments:edit"
        const val PREVIEW_PAYMENT = "payments:preview"
    }
}
