package net.neednot.renttracker.feature.account.domain

sealed interface AccountError {
    data class ValidationFailed(
        val message: String,
        val field: String? = null,
    ) : AccountError

    data object NotSignedIn : AccountError

    data object Offline : AccountError

    data class Unexpected(
        val message: String,
    ) : AccountError
}
