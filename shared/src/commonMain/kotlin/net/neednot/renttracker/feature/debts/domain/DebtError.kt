package net.neednot.renttracker.feature.debts.domain

sealed interface DebtError {
    data class PossibleDuplicate(
        val matches: List<DuplicateLateFee>,
    ) : DebtError

    data class EditConflict(
        val message: String,
        val editedByName: String?,
    ) : DebtError

    data class ValidationFailed(
        val message: String,
    ) : DebtError

    data class PermissionDenied(
        val message: String,
    ) : DebtError

    data class TenantArchived(
        val message: String,
    ) : DebtError

    data object NotFound : DebtError

    data object Offline : DebtError

    data class Unexpected(
        val message: String,
    ) : DebtError
}
