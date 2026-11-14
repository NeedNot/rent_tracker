package net.neednot.renttracker.core.auth

import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.StateFlow

interface AuthSession {
    val state: StateFlow<AuthSessionState>

    suspend fun sendSignInCode(email: String): Result<Unit, AuthSessionError>

    suspend fun signInWithCode(
        email: String,
        code: String,
    ): Result<Unit, AuthSessionError>

    suspend fun signInWithGoogle(token: GoogleIdToken): Result<Unit, AuthSessionError>

    suspend fun refreshJwt(): Result<String, AuthSessionError>

    suspend fun signOut(): Result<Unit, AuthSessionError>
}
