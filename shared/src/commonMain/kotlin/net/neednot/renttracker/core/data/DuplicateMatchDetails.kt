package net.neednot.renttracker.feature.payments.data

import kotlinx.serialization.Serializable

@Serializable
internal data class DuplicateMatchDetails(
    val kind: DuplicateKindDetails,
    val date: String,
    val amountCents: Long,
    val byName: String,
    val byAccountId: String,
    val method: PaymentMethodDetails? = null,
)
