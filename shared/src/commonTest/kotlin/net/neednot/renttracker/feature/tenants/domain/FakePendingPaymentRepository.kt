package net.neednot.renttracker.feature.tenants.domain

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakePendingPaymentRepository : PendingPaymentRepository {
    val pendingPayments = MutableStateFlow<List<PendingPayment>>(emptyList())
    val reviews = MutableStateFlow<Map<String, PendingPaymentReview>>(emptyMap())
    var confirmResult: Result<Unit, TenantError> = Ok(Unit)

    val confirmed = mutableListOf<String>()
    val discarded = mutableListOf<String>()

    override fun observePendingPayments(tenantId: String): Flow<List<PendingPayment>> = pendingPayments

    override fun observeReview(clientId: String): Flow<PendingPaymentReview?> = reviews.map { it[clientId] }

    override suspend fun confirm(clientId: String): Result<Unit, TenantError> {
        confirmed += clientId
        return confirmResult
    }

    override suspend fun discard(clientId: String) {
        discarded += clientId
        reviews.value -= clientId
    }
}
