package net.neednot.renttracker.feature.auth.data

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onErr
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import net.neednot.renttracker.core.auth.AuthSession
import net.neednot.renttracker.core.auth.AuthSessionState
import net.neednot.renttracker.core.auth.GoogleIdTokenProvider
import net.neednot.renttracker.core.backend.Backend
import net.neednot.renttracker.core.backend.BackendAuthState
import net.neednot.renttracker.core.backend.BackendError
import net.neednot.renttracker.core.telemetry.Severity
import net.neednot.renttracker.core.telemetry.Telemetry
import net.neednot.renttracker.feature.auth.domain.AuthError
import net.neednot.renttracker.feature.auth.domain.AuthRepository
import net.neednot.renttracker.feature.auth.domain.SignedInAccount
import net.neednot.renttracker.model.accounts.AccountsEnsureResult
import net.neednot.renttracker.model.accounts.AccountsSetDisplayNameArgs
import kotlin.time.Duration.Companion.seconds

class AuthRepositoryImpl(
    private val authSession: AuthSession,
    private val googleIdTokenProvider: GoogleIdTokenProvider,
    private val backend: Backend,
    private val telemetry: Telemetry,
) : AuthRepository {
    override val sessionState: StateFlow<AuthSessionState> = authSession.state

    override suspend fun sendSignInCode(email: String): Result<Unit, AuthError> =
        authSession.sendSignInCode(email).mapError { it.toAuthError() }.reportingUnexpected()

    override suspend fun signInWithCode(
        email: String,
        code: String,
    ): Result<SignedInAccount, AuthError> =
        authSession
            .signInWithCode(email, code)
            .mapError { it.toAuthError() }
            .andThen { ensureAccountOrSignOut() }
            .reportingUnexpected()

    override suspend fun signInWithGoogle(): Result<SignedInAccount, AuthError> {
        val token =
            googleIdTokenProvider
                .requestIdToken()
                .mapError { it.toAuthError() }
                .reportingUnexpected()
                .getOrElse { return Err(it) }
        return authSession
            .signInWithGoogle(token)
            .mapError { it.toGoogleAuthError() }
            .andThen { ensureAccountOrSignOut() }
            .reportingUnexpected()
    }

    override suspend fun setDisplayName(displayName: String): Result<String, AuthError> {
        val args = AccountsSetDisplayNameArgs(displayName = displayName.trim())
        return onceBackendIsSignedIn {
            backend.mutation(
                name = SET_DISPLAY_NAME,
                args = Json.encodeToJsonElement(AccountsSetDisplayNameArgs.serializer(), args).jsonObject,
                resultSerializer = String.serializer(),
            )
        }.reportingUnexpected()
    }

    override suspend fun signOut(): Result<Unit, AuthError> =
        authSession.signOut().mapError { it.toAuthError() }.reportingUnexpected()

    private suspend fun ensureAccountOrSignOut(): Result<SignedInAccount, AuthError> =
        onceBackendIsSignedIn {
            backend.mutation(
                name = ENSURE,
                args = JsonObject(emptyMap()),
                resultSerializer = AccountsEnsureResult.serializer(),
            )
        }.map { SignedInAccount(displayName = it.displayName, isNew = it.created) }
            .onErr { authSession.signOut() }

    private suspend fun <T> onceBackendIsSignedIn(call: suspend () -> Result<T, BackendError>): Result<T, AuthError> {
        val answer =
            withTimeoutOrNull(BACKEND_TIMEOUT) {
                backend.authState.first { it == BackendAuthState.SignedIn }
                call()
            } ?: return Err(AuthError.Offline)
        return answer.mapError { it.toAuthError() }
    }

    private fun <T> Result<T, AuthError>.reportingUnexpected(): Result<T, AuthError> =
        onErr { error ->
            if (error is AuthError.Unexpected) telemetry.log(TAG, error.message, Severity.Error)
        }

    private companion object {
        const val TAG = "AuthRepository"
        const val ENSURE = "accounts:ensure"
        const val SET_DISPLAY_NAME = "accounts:setDisplayName"

        // The Convex client queues calls while it has no connection instead of failing them.
        val BACKEND_TIMEOUT = 20.seconds
    }
}
