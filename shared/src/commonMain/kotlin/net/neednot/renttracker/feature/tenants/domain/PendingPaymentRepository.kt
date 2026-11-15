package net.neednot.renttracker.feature.tenants.domain

import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.Flow

interface PendingPaymentRepository {
    fun observePendingPayments(tenantId: String): Flow<List<PendingPayment>>

    fun observeReview(clientId: String): Flow<PendingPaymentReview?>

    suspend fun confirm(clientId: String): Result<Unit, TenantError>

    suspend fun discard(clientId: String)
}
