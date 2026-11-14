package net.neednot.renttracker.core.auth

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NotYetSupportedAuthSession : AuthSession {
    override val state: StateFlow<AuthSessionState> =
        MutableStateFlow<AuthSessionState>(AuthSessionState.SignedOut).asStateFlow()

    override suspend fun sendSignInCode(email: String): Result<Unit, AuthSessionError> = notYetSupported()

    override suspend fun signInWithCode(
        email: String,
        code: String,
    ): Result<Unit, AuthSessionError> = notYetSupported()

    override suspend fun signInWithGoogle(token: GoogleIdToken): Result<Unit, AuthSessionError> = notYetSupported()

    override suspend fun refreshJwt(): Result<String, AuthSessionError> = notYetSupported()

    override suspend fun signOut(): Result<Unit, AuthSessionError> = notYetSupported()

    private fun notYetSupported(): Result<Nothing, AuthSessionError> =
        Err(AuthSessionError.UnexpectedResponse("Sign-in is not supported on iOS yet"))
}
