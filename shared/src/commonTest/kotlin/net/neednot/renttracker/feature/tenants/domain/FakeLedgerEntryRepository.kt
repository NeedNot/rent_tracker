package net.neednot.renttracker.feature.tenants.domain

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlinx.datetime.LocalDate

class FakeLedgerEntryRepository : LedgerEntryRepository {
    var changeResult: Result<Unit, TenantError> = Ok(Unit)

    val calls = mutableListOf<TenantRepositoryCall>()

    override suspend fun cancelPayment(
        paymentId: String,
        expectedVersion: Long,
        reason: String,
    ): Result<Unit, TenantError> {
        calls += TenantRepositoryCall.CancelPayment(paymentId, expectedVersion, reason)
        return changeResult
    }

    override suspend fun restorePayment(
        paymentId: String,
        expectedVersion: Long,
    ): Result<Unit, TenantError> {
        calls += TenantRepositoryCall.RestorePayment(paymentId, expectedVersion)
        return changeResult
    }

    override suspend fun markPaymentBounced(
        paymentId: String,
        expectedVersion: Long,
    ): Result<Unit, TenantError> {
        calls += TenantRepositoryCall.MarkPaymentBounced(paymentId, expectedVersion)
        return changeResult
    }

    override suspend fun cancelDebt(
        debtId: String,
        expectedVersion: Long,
        reason: String,
    ): Result<Unit, TenantError> {
        calls += TenantRepositoryCall.CancelDebt(debtId, expectedVersion, reason)
        return changeResult
    }

    override suspend fun restoreDebt(
        debtId: String,
        expectedVersion: Long,
    ): Result<Unit, TenantError> {
        calls += TenantRepositoryCall.RestoreDebt(debtId, expectedVersion)
        return changeResult
    }

    override suspend fun addBouncedCheckFee(
        tenantId: String,
        amountCents: Long,
        date: LocalDate,
    ): Result<Unit, TenantError> {
        calls += TenantRepositoryCall.AddBouncedCheckFee(tenantId, amountCents, date)
        return changeResult
    }
}
