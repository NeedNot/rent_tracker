package net.neednot.renttracker.feature.payments.presentation.viewmodel

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import net.neednot.renttracker.feature.payments.domain.DuplicatePolicy
import net.neednot.renttracker.feature.payments.domain.PaymentError
import net.neednot.renttracker.feature.payments.domain.PaymentRepository

internal suspend fun PaymentRepository.save(
    request: PaymentSaveRequest,
    form: ValidPaymentForm,
    duplicatePolicy: DuplicatePolicy,
): Result<Unit, PaymentError> {
    val paymentId = request.paymentId
    val version = request.expectedVersion
    return when {
        paymentId == null -> {
            logPayment(request.clientId, request.tenantId, form.draft, form.lateFee, duplicatePolicy).map { }
        }

        version == null -> {
            Err(PaymentError.NotFound)
        }

        else -> {
            editPayment(paymentId, version, form.draft)
        }
    }
}
