package net.neednot.renttracker.backend

import android.content.Context
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import dev.convex.android.AuthState
import dev.convex.android.ConvexClientWithAuth
import dev.convex.android.ConvexError
import dev.convex.android.InternalError
import dev.convex.android.ServerError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import net.neednot.renttracker.core.auth.AuthSession
import net.neednot.renttracker.core.auth.AuthSessionState
import net.neednot.renttracker.core.backend.Backend
import net.neednot.renttracker.core.backend.BackendAuthState
import net.neednot.renttracker.core.backend.BackendError
import net.neednot.renttracker.core.backend.ConvexJson
import net.neednot.renttracker.core.telemetry.Severity
import net.neednot.renttracker.core.telemetry.Telemetry
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class ConvexBackend(
    deploymentUrl: String,
    private val context: Context,
    private val authSession: AuthSession,
    private val telemetry: Telemetry,
) : Backend {
    init {
        require(deploymentUrl.isNotBlank()) { "convex.url is missing from local.properties" }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val client = ConvexClientWithAuth(deploymentUrl, SessionJwtProvider(authSession))

    override val authState: StateFlow<BackendAuthState> =
        combine(authSession.state, client.authState, ::backendAuthState)
            .stateIn(scope, SharingStarted.Eagerly, BackendAuthState.Loading)

    init {
        scope.launch { followSession() }
    }

    override fun <T> subscribe(
        name: String,
        args: JsonObject,
        resultSerializer: KSerializer<T>,
    ): Flow<Result<T, BackendError>> =
        client.subscribe<JsonElement>(name, ConvexJson.toClientArgs(args)).map { update ->
            update.fold(
                onSuccess = { value -> decode(resultSerializer, value) },
                onFailure = { error -> Err(error.toBackendError()) },
            )
        }

    override suspend fun <T> mutation(
        name: String,
        args: JsonObject,
        resultSerializer: KSerializer<T>,
    ): Result<T, BackendError> =
        call(resultSerializer) {
            client.mutation<JsonElement>(name, ConvexJson.toClientArgs(args))
        }

    override suspend fun <T> action(
        name: String,
        args: JsonObject,
        resultSerializer: KSerializer<T>,
    ): Result<T, BackendError> =
        call(resultSerializer) { client.action<JsonElement>(name, ConvexJson.toClientArgs(args)) }

    private fun backendAuthState(
        session: AuthSessionState,
        convex: AuthState<String>,
    ): BackendAuthState =
        when (session) {
            AuthSessionState.Loading -> {
                BackendAuthState.Loading
            }

            AuthSessionState.SignedOut -> {
                BackendAuthState.SignedOut
            }

            is AuthSessionState.SignedIn -> {
                if (convex is AuthState.Authenticated) BackendAuthState.SignedIn else BackendAuthState.Loading
            }
        }

    private suspend fun followSession() {
        authSession.state.collectLatest { session ->
            when (session) {
                AuthSessionState.Loading -> Unit

                // Not cancellable, so the next person to sign in never inherits the previous person's token.
                AuthSessionState.SignedOut -> withContext(NonCancellable) { client.logout(context) }

                is AuthSessionState.SignedIn -> keepConvexSignedIn()
            }
        }
    }

    private suspend fun keepConvexSignedIn() {
        var retryDelay = Duration.ZERO
        presentJwt()
        client.authState.collect { convex ->
            if (convex is AuthState.Authenticated) retryDelay = Duration.ZERO
            if (convex is AuthState.Unauthenticated) {
                retryDelay = (retryDelay * 2).coerceIn(FIRST_RETRY_DELAY, LONGEST_RETRY_DELAY)
                delay(retryDelay)
                presentJwt()
            }
        }
    }

    private suspend fun presentJwt() {
        client.loginFromCache().onFailure { error ->
            telemetry.log(TAG, "Convex sign-in failed: ${error.message}", Severity.Warning)
        }
    }

    private suspend fun <T> call(
        resultSerializer: KSerializer<T>,
        request: suspend () -> JsonElement,
    ): Result<T, BackendError> =
        try {
            decode(resultSerializer, request())
        } catch (error: ConvexError) {
            Err(error.toBackendError())
        } catch (error: ServerError) {
            Err(error.toBackendError())
        } catch (error: InternalError) {
            Err(error.toBackendError())
        }

    private fun <T> decode(
        resultSerializer: KSerializer<T>,
        value: JsonElement,
    ): Result<T, BackendError> =
        try {
            Ok(ConvexJson.decode(resultSerializer, value))
        } catch (error: SerializationException) {
            Err(BackendError.Internal("Could not decode result: ${error.message}"))
        } catch (error: IllegalArgumentException) {
            Err(BackendError.Internal("Could not decode result: ${error.message}"))
        }

    private fun Throwable.toBackendError(): BackendError =
        when (this) {
            is ConvexError -> ConvexJson.parseError(data)
            is ServerError -> BackendError.Server(message.orEmpty())
            else -> BackendError.Internal(message ?: toString())
        }

    private companion object {
        const val TAG = "ConvexBackend"
        val FIRST_RETRY_DELAY = 2.seconds
        val LONGEST_RETRY_DELAY = 60.seconds
    }
}
