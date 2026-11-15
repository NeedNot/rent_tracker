package net.neednot.renttracker.feature.payments.data

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import net.neednot.renttracker.core.data.Connectivity
import net.neednot.renttracker.core.data.PendingPaymentQueue
import net.neednot.renttracker.feature.payments.domain.LoggedPayment
import net.neednot.renttracker.feature.payments.domain.PaymentDraft
import net.neednot.renttracker.feature.payments.domain.PaymentError

class PhoneOutbox(
    private val connectivity: Connectivity,
    private val pendingPaymentQueue: PendingPaymentQueue,
) {
    val isOffline: Boolean get() = !connectivity.isOnline.value

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
