package net.neednot.renttracker.feature.payments.domain

import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import net.neednot.renttracker.core.sync.Synced

interface PaymentRepository {
    fun observeEntry(
        tenantId: String,
        paymentId: String?,
    ): Flow<Synced<PaymentEntryOverview?, PaymentError>>

    suspend fun logPayment(
        clientId: String,
        tenantId: String,
        draft: PaymentDraft,
        lateFee: LateFeeDraft?,
        duplicatePolicy: DuplicatePolicy,
    ): Result<String, PaymentError>

    suspend fun editPayment(
        paymentId: String,
        expectedVersion: Long,
        draft: PaymentDraft,
    ): Result<Unit, PaymentError>

    suspend fun previewPaymentChange(
        tenantId: String,
        paymentId: String,
        amountCents: Long,
        dateReceived: LocalDate,
    ): Result<List<MonthStateChange>, PaymentError>
}
