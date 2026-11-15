package net.neednot.renttracker.feature.group.domain

sealed interface GroupError {
    data object NotFound : GroupError

    data object NoAccess : GroupError

    data object Offline : GroupError

    data class ValidationFailed(
        val message: String,
        val field: String? = null,
    ) : GroupError

    data class EditConflict(
        val message: String,
        val editedByName: String?,
    ) : GroupError

    data class Unexpected(
        val message: String,
    ) : GroupError
}
