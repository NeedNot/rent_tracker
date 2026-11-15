package net.neednot.renttracker.feature.group.data

import kotlinx.serialization.Serializable

@Serializable
internal data class EditConflictDetails(
    val lastEditedByName: String? = null,
)
