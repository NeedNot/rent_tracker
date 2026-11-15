package net.neednot.renttracker.feature.payments.data

import kotlinx.serialization.Serializable

@Serializable
internal data class NeedsConfirmationDetails(
    val matches: List<DuplicateMatchDetails>,
)
