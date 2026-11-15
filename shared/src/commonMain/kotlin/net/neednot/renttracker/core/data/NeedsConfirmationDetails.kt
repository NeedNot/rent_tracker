package net.neednot.renttracker.core.data

import kotlinx.serialization.Serializable

@Serializable
data class NeedsConfirmationDetails(
    val matches: List<DuplicateMatchDetails>,
)
