package net.neednot.renttracker.feature.members.domain

sealed interface MembersError {
    data class ValidationFailed(
        val message: String,
        val field: String? = null,
    ) : MembersError

    data class EditConflict(
        val message: String,
        val editedByName: String?,
    ) : MembersError

    data class PermissionDenied(
        val message: String,
    ) : MembersError

    data class NotFound(
        val message: String,
    ) : MembersError

    data object Offline : MembersError

    data class Unexpected(
        val message: String,
    ) : MembersError
}
