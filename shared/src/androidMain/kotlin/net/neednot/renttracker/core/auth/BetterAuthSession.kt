package net.neednot.renttracker.core.auth

import android.content.Context
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.neednot.betterauth.AuthConfig
import net.neednot.betterauth.AuthError
import net.neednot.betterauth.AuthState
import net.neednot.betterauth.BetterAuthClient
import net.neednot.betterauth.sendSignInCode
import net.neednot.betterauth.signInWithCode
import net.neednot.betterauth.signInWithIdToken

class BetterAuthSession(
    context: Context,
    authUrl: String,
) : AuthSession {
    init {
        require(authUrl.isNotBlank()) { "auth.url is missing from local.properties" }
    }

    private val client =
        BetterAuthClient(
            context.applicationContext,
            AuthConfig(baseUrl = authUrl.trimEnd('/') + AUTH_PATH, origin = APP_ORIGIN),
        )

    override val state: StateFlow<AuthSessionState> =
        client.state
            .map { it.toSessionState() }
            .stateIn(
                scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
                started = SharingStarted.Eagerly,
                initialValue = client.state.value.toSessionState(),
            )

    override suspend fun sendSignInCode(email: String): Result<Unit, AuthSessionError> =
        client.sendSignInCode(email).mapError { it.toSessionError() }

    override suspend fun signInWithCode(
        email: String,
        code: String,
    ): Result<Unit, AuthSessionError> = client.signInWithCode(email, code).map { }.mapError { it.toSessionError() }

    override suspend fun signInWithGoogle(token: GoogleIdToken): Result<Unit, AuthSessionError> =
        client
            .signInWithIdToken(provider = GOOGLE, idToken = token.idToken, nonce = token.nonce)
            .map { }
            .mapError { it.toSessionError() }

    override suspend fun refreshJwt(): Result<String, AuthSessionError> =
        client.refreshJwt().mapError { it.toSessionError() }

    override suspend fun signOut(): Result<Unit, AuthSessionError> = client.signOut().mapError { it.toSessionError() }

    private fun AuthState.toSessionState(): AuthSessionState =
        when (this) {
            AuthState.Loading -> AuthSessionState.Loading
            AuthState.SignedOut -> AuthSessionState.SignedOut
            is AuthState.SignedIn -> AuthSessionState.SignedIn(userId)
        }

    private fun AuthError.toSessionError(): AuthSessionError =
        when (this) {
            AuthError.NotSignedIn -> AuthSessionError.NotSignedIn
            AuthError.SessionExpired -> AuthSessionError.SessionExpired
            AuthError.InvalidCode -> AuthSessionError.InvalidCode
            AuthError.CodeExpired -> AuthSessionError.CodeExpired
            AuthError.TooManyAttempts -> AuthSessionError.TooManyAttempts
            is AuthError.RateLimited -> AuthSessionError.RateLimited(retryAfterSeconds)
            AuthError.Network -> AuthSessionError.Network
            AuthError.Storage -> AuthSessionError.Storage
            is AuthError.Server -> AuthSessionError.Server(status, code, message)
            is AuthError.UnexpectedResponse -> AuthSessionError.UnexpectedResponse(message)
        }

    private companion object {
        const val AUTH_PATH = "/api/auth"
        const val APP_ORIGIN = "rent-tracker://app"
        const val GOOGLE = "google"
    }
}
