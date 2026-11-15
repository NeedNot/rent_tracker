package net.neednot.renttracker.backend

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.MutableStateFlow
import net.neednot.renttracker.core.auth.AuthSession
import net.neednot.renttracker.core.auth.AuthSessionError
import net.neednot.renttracker.core.auth.AuthSessionState
import net.neednot.renttracker.core.auth.GoogleIdToken

class SignedInAuthSession : AuthSession {
    override val state = MutableStateFlow<AuthSessionState>(AuthSessionState.SignedIn("user-1"))

    override suspend fun sendSignInCode(email: String): Result<Unit, AuthSessionError> = Ok(Unit)

    override suspend fun signInWithCode(
        email: String,
        code: String,
    ): Result<Unit, AuthSessionError> = Ok(Unit)

    override suspend fun signInWithGoogle(token: GoogleIdToken): Result<Unit, AuthSessionError> = Ok(Unit)

    override suspend fun refreshJwt(): Result<String, AuthSessionError> = Ok("jwt")

    override suspend fun signOut(): Result<Unit, AuthSessionError> = Ok(Unit)
}
