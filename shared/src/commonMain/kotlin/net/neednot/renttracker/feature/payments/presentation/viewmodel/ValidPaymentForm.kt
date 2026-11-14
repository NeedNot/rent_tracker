package net.neednot.renttracker.feature.payments.presentation.viewmodel

import net.neednot.renttracker.feature.payments.domain.LateFeeDraft
import net.neednot.renttracker.feature.payments.domain.PaymentDraft

internal data class ValidPaymentForm(
    val draft: PaymentDraft,
    val lateFee: LateFeeDraft?,
)
