package net.neednot.renttracker.feature.payments.domain

sealed interface PaymentRepositoryCall {
    data class Log(
        val clientId: String,
        val tenantId: String,
        val draft: PaymentDraft,
        val lateFee: LateFeeDraft?,
        val duplicatePolicy: DuplicatePolicy,
    ) : PaymentRepositoryCall

    data class Edit(
        val paymentId: String,
        val expectedVersion: Long,
        val draft: PaymentDraft,
    ) : PaymentRepositoryCall
}
