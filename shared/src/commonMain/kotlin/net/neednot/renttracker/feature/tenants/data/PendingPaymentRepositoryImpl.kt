package net.neednot.renttracker.feature.tenants.data

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import net.neednot.renttracker.core.data.PendingPaymentEntity
import net.neednot.renttracker.core.data.PendingPaymentQueue
import net.neednot.renttracker.core.data.PendingPaymentReader
import net.neednot.renttracker.core.data.PendingQueueError
import net.neednot.renttracker.core.data.RentTrackerDatabase
import net.neednot.renttracker.feature.tenants.domain.PendingPayment
import net.neednot.renttracker.feature.tenants.domain.PendingPaymentRepository
import net.neednot.renttracker.feature.tenants.domain.PendingPaymentReview
import net.neednot.renttracker.feature.tenants.domain.PendingState
import net.neednot.renttracker.feature.tenants.domain.TenantError

class PendingPaymentRepositoryImpl(
    private val database: RentTrackerDatabase,
    private val reader: PendingPaymentReader,
    private val queue: PendingPaymentQueue,
) : PendingPaymentRepository {
    override fun observePendingPayments(tenantId: String): Flow<List<PendingPayment>> =
        reader.observeForTenant(tenantId).map { payments -> payments.map { it.toPendingPayment() } }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeReview(clientId: String): Flow<PendingPaymentReview?> =
        reader.observe(clientId).flatMapLatest { entity ->
            if (entity == null) flowOf(null) else reviewOf(entity)
        }

    override suspend fun confirm(clientId: String): Result<Unit, TenantError> =
        queue.confirm(clientId).mapError { it.toTenantError() }

    override suspend fun discard(clientId: String) {
        queue.discard(clientId)
    }

    private fun reviewOf(entity: PendingPaymentEntity): Flow<PendingPaymentReview> {
        val payment = entity.toPendingPayment()
        return when (val state = payment.state) {
            PendingState.Waiting -> {
                flowOf(PendingPaymentReview.Waiting(payment))
            }

            PendingState.PossibleDuplicate -> {
                reader.observeDuplicateMatches(entity.clientId).map { matches ->
                    PendingPaymentReview.PossibleDuplicate(payment, matches.map { it.toDuplicateMatch() })
                }
            }

            PendingState.TenantChanged -> {
                observeTenantChanges(entity).map { changes ->
                    PendingPaymentReview.TenantChanged(
                        payment = payment,
                        balanceChange = changes.balanceChange(entity.balanceCentsWhenLogged),
                        newEntries = changes.newEntriesSince(entity.tenantFingerprint),
                    )
                }
            }

            is PendingState.Rejected -> {
                flowOf(PendingPaymentReview.Rejected(payment, state.reason))
            }
        }
    }

    private fun observeTenantChanges(entity: PendingPaymentEntity): Flow<PendingTenantChanges> =
        combine(
            database.tenantSummaryDao().observeByTenantId(entity.tenantId),
            database.paymentDao().observeByTenantId(entity.tenantId),
            database.debtDao().observeByTenantId(entity.tenantId),
            database.accountNameDao().observeAll(),
            database.membershipDao().observeCaller(entity.groupId),
            ::PendingTenantChanges,
        )
}

private fun PendingQueueError.toTenantError(): TenantError =
    when (this) {
        PendingQueueError.NotFound, PendingQueueError.TenantNotCached -> TenantError.NotFound
        PendingQueueError.NotEditable -> TenantError.Unexpected("A rejected payment can't be confirmed")
    }
