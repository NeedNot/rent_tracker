package net.neednot.renttracker.feature.payments.data

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.neednot.renttracker.core.data.CacheScope
import net.neednot.renttracker.core.data.Connectivity
import net.neednot.renttracker.core.data.FreshnessSource
import net.neednot.renttracker.core.data.PendingPaymentQueue
import net.neednot.renttracker.core.data.PendingPaymentReader
import net.neednot.renttracker.core.sync.Freshness
import net.neednot.renttracker.feature.payments.domain.LoggedPayment
import net.neednot.renttracker.feature.payments.domain.PaymentDraft
import net.neednot.renttracker.feature.payments.domain.PaymentError
import net.neednot.renttracker.feature.payments.domain.PendingPayment

class PhoneOutbox(
    private val connectivity: Connectivity,
    private val pendingPaymentQueue: PendingPaymentQueue,
    private val pendingPaymentReader: PendingPaymentReader,
    private val freshnessSource: FreshnessSource,
) {
    val isOffline: Boolean get() = !connectivity.isOnline.value

    fun observeFreshness(tenantId: String): Flow<Freshness> =
        freshnessSource.observe(listOf(CacheScope.tenant(tenantId)))

    fun observe(clientId: String): Flow<PendingPayment?> =
        pendingPaymentReader.observe(clientId).map { it?.toPendingPayment() }

    suspend fun edit(
        clientId: String,
        draft: PaymentDraft,
    ): Result<Unit, PaymentError> =
        pendingPaymentQueue.edit(clientId, draft.toPendingDetails()).mapError { it.toPaymentError() }

    suspend fun keep(
        clientId: String,
        tenantId: String,
        draft: PaymentDraft,
    ): Result<LoggedPayment, PaymentError> =
        pendingPaymentQueue
            .enqueue(clientId, tenantId, draft.toPendingDetails())
            .map { LoggedPayment.SavedOnPhone(clientId) }
            .mapError { PaymentError.Offline }
}
