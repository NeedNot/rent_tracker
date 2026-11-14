package net.neednot.renttracker.feature.payments.domain

sealed interface PaymentError {
    data class PossibleDuplicate(
        val matches: List<DuplicateMatch>,
    ) : PaymentError

    data class EditConflict(
        val message: String,
        val editedByName: String?,
    ) : PaymentError

    data class ValidationFailed(
        val message: String,
        val field: String? = null,
    ) : PaymentError

    data class PermissionDenied(
        val message: String,
    ) : PaymentError

    data class TenantArchived(
        val message: String,
    ) : PaymentError

    data object NotFound : PaymentError

    data object Offline : PaymentError

    data class Unexpected(
        val message: String,
    ) : PaymentError
}
