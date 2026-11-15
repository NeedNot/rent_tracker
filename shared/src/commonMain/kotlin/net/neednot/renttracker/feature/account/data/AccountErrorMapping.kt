package net.neednot.renttracker.feature.account.data

import net.neednot.renttracker.core.auth.AuthSessionError
import net.neednot.renttracker.core.backend.BackendError
import net.neednot.renttracker.core.backend.validationField
import net.neednot.renttracker.feature.account.domain.AccountError

private const val VALIDATION_FAILED = "ValidationFailed"
private const val NOT_SIGNED_IN = "NotSignedIn"

internal fun BackendError.toAccountError(): AccountError =
    when (this) {
        is BackendError.Convex -> toAccountError()
        is BackendError.Server -> AccountError.Unexpected(message)
        is BackendError.Internal -> AccountError.Unexpected(message)
    }

private fun BackendError.Convex.toAccountError(): AccountError =
    when (code) {
        VALIDATION_FAILED -> AccountError.ValidationFailed(message, validationField())
        NOT_SIGNED_IN -> AccountError.NotSignedIn
        else -> AccountError.Unexpected("$code: $message")
    }

internal fun AuthSessionError.toAccountError(): AccountError =
    when (this) {
        AuthSessionError.NotSignedIn, AuthSessionError.SessionExpired -> AccountError.NotSignedIn
        AuthSessionError.Network -> AccountError.Offline
        else -> AccountError.Unexpected("Sign-out failed: $this")
    }
