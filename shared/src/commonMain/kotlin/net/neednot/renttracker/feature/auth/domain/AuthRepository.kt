package net.neednot.renttracker.feature.auth.domain

import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.StateFlow
import net.neednot.renttracker.core.auth.AuthSessionState

interface AuthRepository {
    val sessionState: StateFlow<AuthSessionState>

    suspend fun sendSignInCode(email: String): Result<Unit, AuthError>

    suspend fun signInWithCode(
        email: String,
        code: String,
    ): Result<SignedInAccount, AuthError>

    suspend fun signInWithGoogle(): Result<SignedInAccount, AuthError>

    suspend fun setDisplayName(displayName: String): Result<String, AuthError>

    suspend fun signOut(): Result<Unit, AuthError>
}
