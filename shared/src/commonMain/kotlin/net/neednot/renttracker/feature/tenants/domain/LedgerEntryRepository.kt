package net.neednot.renttracker.feature.tenants.domain

import com.github.michaelbull.result.Result
import kotlinx.datetime.LocalDate

interface LedgerEntryRepository {
    suspend fun cancelPayment(
        paymentId: String,
        expectedVersion: Long,
        reason: String,
    ): Result<Unit, TenantError>

    suspend fun restorePayment(
        paymentId: String,
        expectedVersion: Long,
    ): Result<Unit, TenantError>

    suspend fun markPaymentBounced(
        paymentId: String,
        expectedVersion: Long,
    ): Result<Unit, TenantError>

    suspend fun cancelDebt(
        debtId: String,
        expectedVersion: Long,
        reason: String,
    ): Result<Unit, TenantError>

    suspend fun restoreDebt(
        debtId: String,
        expectedVersion: Long,
    ): Result<Unit, TenantError>

    suspend fun addBouncedCheckFee(
        tenantId: String,
        amountCents: Long,
        date: LocalDate,
    ): Result<Unit, TenantError>
}
