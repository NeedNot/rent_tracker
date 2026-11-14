package net.neednot.renttracker.core.auth

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result

class FakeGoogleIdTokenProvider : GoogleIdTokenProvider {
    var result: Result<GoogleIdToken, GoogleIdTokenError> =
        Ok(GoogleIdToken(idToken = "google-id-token", nonce = "nonce"))

    override suspend fun requestIdToken(): Result<GoogleIdToken, GoogleIdTokenError> = result
}
