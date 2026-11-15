package net.neednot.renttracker.feature.payments.presentation.viewmodel

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import net.neednot.renttracker.feature.payments.domain.DuplicatePolicy
import net.neednot.renttracker.feature.payments.domain.LoggedPayment
import net.neednot.renttracker.feature.payments.domain.PaymentError
import net.neednot.renttracker.feature.payments.domain.PaymentRepository

internal suspend fun PaymentRepository.save(
    request: PaymentSaveRequest,
    form: ValidPaymentForm,
    duplicatePolicy: DuplicatePolicy,
): Result<PaymentSaveOutcome, PaymentError> {
    val paymentId = request.paymentId
    val version = request.expectedVersion
    val pendingClientId = request.pendingClientId
    return when {
        pendingClientId != null -> {
            editPendingPayment(pendingClientId, form.draft).map { PaymentSaveOutcome.Synced }
        }

        paymentId == null -> {
            logPayment(request.clientId, request.tenantId, form.draft, form.lateFee, duplicatePolicy)
                .map { logged -> logged.toOutcome() }
        }

        version == null -> {
            Err(PaymentError.NotFound)
        }

        else -> {
            editPayment(paymentId, version, form.draft).map { PaymentSaveOutcome.Synced }
        }
    }
}

private fun LoggedPayment.toOutcome(): PaymentSaveOutcome =
    when (this) {
        is LoggedPayment.Saved -> PaymentSaveOutcome.Synced
        is LoggedPayment.SavedOnPhone -> PaymentSaveOutcome.SavedOnPhone
    }
