package net.neednot.renttracker.feature.auth.data

import net.neednot.renttracker.core.auth.AuthSessionError
import net.neednot.renttracker.core.auth.GoogleIdTokenError
import net.neednot.renttracker.core.backend.BackendError
import net.neednot.renttracker.feature.auth.domain.AuthError

private const val NOT_SIGNED_IN = "NotSignedIn"
private const val VALIDATION_FAILED = "ValidationFailed"

internal fun AuthSessionError.toAuthError(): AuthError =
    when (this) {
        AuthSessionError.NotSignedIn, AuthSessionError.SessionExpired -> AuthError.SessionExpired
        AuthSessionError.InvalidCode -> AuthError.InvalidCode
        AuthSessionError.CodeExpired -> AuthError.CodeExpired
        AuthSessionError.TooManyAttempts -> AuthError.TooManyAttempts
        is AuthSessionError.RateLimited -> AuthError.RateLimited(retryAfterSeconds)
        AuthSessionError.Network -> AuthError.Offline
        AuthSessionError.Storage -> AuthError.Unexpected("The session could not be stored on this phone")
        is AuthSessionError.Server -> AuthError.Unexpected("Auth server answered $status $code: $message")
        is AuthSessionError.UnexpectedResponse -> AuthError.Unexpected(message)
    }

internal fun AuthSessionError.toGoogleAuthError(): AuthError =
    when (this) {
        is AuthSessionError.Server -> AuthError.GoogleSignInFailed
        else -> toAuthError()
    }

internal fun GoogleIdTokenError.toAuthError(): AuthError =
    when (this) {
        GoogleIdTokenError.Cancelled -> AuthError.GoogleSignInCancelled
        is GoogleIdTokenError.Failed -> AuthError.GoogleSignInFailed
    }

internal fun BackendError.toAuthError(): AuthError =
    when (this) {
        is BackendError.Convex -> {
            when (code) {
                NOT_SIGNED_IN -> AuthError.SessionExpired
                VALIDATION_FAILED -> AuthError.InvalidDisplayName
                else -> AuthError.Unexpected("$code: $message")
            }
        }

        is BackendError.Server -> {
            AuthError.Unexpected(message)
        }

        is BackendError.Internal -> {
            AuthError.Unexpected(message)
        }
    }
