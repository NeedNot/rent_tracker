package net.neednot.renttracker.feature.tenants.data

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import kotlinx.datetime.LocalDate
import kotlinx.serialization.builtins.serializer
import net.neednot.renttracker.core.backend.Backend
import net.neednot.renttracker.core.telemetry.Telemetry
import net.neednot.renttracker.feature.tenants.domain.LedgerEntryRepository
import net.neednot.renttracker.feature.tenants.domain.TenantError
import net.neednot.renttracker.model.debts.DebtsAddCustomArgs
import net.neednot.renttracker.model.debts.DebtsDeleteArgs
import net.neednot.renttracker.model.debts.DebtsRestoreArgs
import net.neednot.renttracker.model.payments.PaymentsDeleteArgs
import net.neednot.renttracker.model.payments.PaymentsMarkBouncedArgs
import net.neednot.renttracker.model.payments.PaymentsRestoreArgs
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class LedgerEntryRepositoryImpl(
    backend: Backend,
    telemetry: Telemetry,
) : LedgerEntryRepository {
    private val calls = TenantBackendCalls(backend, telemetry)

    override suspend fun cancelPayment(
        paymentId: String,
        expectedVersion: Long,
        reason: String,
    ): Result<Unit, TenantError> =
        calls.mutateWithoutAnswer(
            name = DELETE_PAYMENT,
            args = PaymentsDeleteArgs(expectedVersion = expectedVersion, paymentId = paymentId, reason = reason),
            argsSerializer = PaymentsDeleteArgs.serializer(),
        )

    override suspend fun restorePayment(
        paymentId: String,
        expectedVersion: Long,
    ): Result<Unit, TenantError> =
        calls.mutateWithoutAnswer(
            name = RESTORE_PAYMENT,
            args = PaymentsRestoreArgs(expectedVersion = expectedVersion, paymentId = paymentId),
            argsSerializer = PaymentsRestoreArgs.serializer(),
        )

    override suspend fun markPaymentBounced(
        paymentId: String,
        expectedVersion: Long,
    ): Result<Unit, TenantError> =
        calls.mutateWithoutAnswer(
            name = MARK_PAYMENT_BOUNCED,
            args = PaymentsMarkBouncedArgs(expectedVersion = expectedVersion, paymentId = paymentId),
            argsSerializer = PaymentsMarkBouncedArgs.serializer(),
        )

    override suspend fun cancelDebt(
        debtId: String,
        expectedVersion: Long,
        reason: String,
    ): Result<Unit, TenantError> =
        calls.mutateWithoutAnswer(
            name = DELETE_DEBT,
            args = DebtsDeleteArgs(debtId = debtId, expectedVersion = expectedVersion, reason = reason),
            argsSerializer = DebtsDeleteArgs.serializer(),
        )

    override suspend fun restoreDebt(
        debtId: String,
        expectedVersion: Long,
    ): Result<Unit, TenantError> =
        calls.mutateWithoutAnswer(
            name = RESTORE_DEBT,
            args = DebtsRestoreArgs(debtId = debtId, expectedVersion = expectedVersion),
            argsSerializer = DebtsRestoreArgs.serializer(),
        )

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun addBouncedCheckFee(
        tenantId: String,
        amountCents: Long,
        date: LocalDate,
    ): Result<Unit, TenantError> =
        calls
            .mutate(
                name = ADD_CUSTOM_DEBT,
                args =
                    DebtsAddCustomArgs(
                        amountCents = amountCents,
                        clientId = Uuid.random().toString(),
                        date = date.toString(),
                        note = BOUNCED_CHECK_FEE_NOTE,
                        tenantId = tenantId,
                    ),
                argsSerializer = DebtsAddCustomArgs.serializer(),
                resultSerializer = String.serializer(),
            ).map { }

    private companion object {
        const val DELETE_PAYMENT = "payments:delete"
        const val RESTORE_PAYMENT = "payments:restore"
        const val MARK_PAYMENT_BOUNCED = "payments:markBounced"
        const val DELETE_DEBT = "debts:delete"
        const val RESTORE_DEBT = "debts:restore"
        const val ADD_CUSTOM_DEBT = "debts:addCustom"
        const val BOUNCED_CHECK_FEE_NOTE = "Bounced-check fee"
    }
}
