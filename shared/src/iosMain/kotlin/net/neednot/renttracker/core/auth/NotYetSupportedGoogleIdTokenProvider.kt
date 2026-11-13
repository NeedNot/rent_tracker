package net.neednot.renttracker.core.auth

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result

class NotYetSupportedGoogleIdTokenProvider : GoogleIdTokenProvider {
    override suspend fun requestIdToken(): Result<GoogleIdToken, GoogleIdTokenError> =
        Err(GoogleIdTokenError.Failed("Google sign-in is not supported on iOS yet"))
}
