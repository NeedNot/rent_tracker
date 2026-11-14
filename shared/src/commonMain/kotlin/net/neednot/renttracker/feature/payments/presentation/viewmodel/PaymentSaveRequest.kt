package net.neednot.renttracker.feature.payments.presentation.viewmodel

internal data class PaymentSaveRequest(
    val tenantId: String,
    val paymentId: String?,
    val clientId: String,
    val expectedVersion: Long?,
)
