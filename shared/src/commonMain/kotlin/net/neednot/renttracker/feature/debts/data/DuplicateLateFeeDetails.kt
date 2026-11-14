package net.neednot.renttracker.feature.debts.data

import kotlinx.serialization.Serializable

@Serializable
internal data class DuplicateLateFeeDetails(
    val date: String,
    val amountCents: Long,
    val byName: String,
    val byAccountId: String,
)
