package net.neednot.renttracker.core.data

import kotlinx.serialization.Serializable

@Serializable
data class DuplicateMatchDetails(
    val id: String,
    val kind: DuplicateKindDetails,
    val date: String,
    val amountCents: Long,
    val byName: String,
    val byAccountId: String,
    val method: PaymentMethodDetails? = null,
)
