package net.neednot.renttracker.core.data

import com.github.michaelbull.result.onErr
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import net.neednot.renttracker.core.backend.Backend
import net.neednot.renttracker.core.backend.BackendAuthState
import net.neednot.renttracker.core.backend.ConvexJson
import net.neednot.renttracker.core.telemetry.Severity
import net.neednot.renttracker.core.telemetry.Telemetry
import net.neednot.renttracker.model.pushtokens.PushTokensRegisterArgs
import net.neednot.renttracker.model.pushtokens.PushTokensRegisterPlatform
import net.neednot.renttracker.model.pushtokens.PushTokensUnregisterArgs
import kotlin.time.Duration.Companion.seconds

class PushTokenRegistration(
    private val backend: Backend,
    private val pushTokens: PushTokens,
    private val telemetry: Telemetry,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun registerWhileSignedIn() {
        backend.authState
            .flatMapLatest { state -> if (state == BackendAuthState.SignedIn) tokensToRegister() else emptyFlow() }
            .collect { token -> register(token) }
    }

    suspend fun unregisterCurrentToken() {
        withTimeoutOrNull(UNREGISTER_TIMEOUT) {
            val token = pushTokens.currentToken() ?: return@withTimeoutOrNull
            val args = PushTokensUnregisterArgs(token = token)
            mutate(UNREGISTER, ConvexJson.encodeArgs(PushTokensUnregisterArgs.serializer(), args))
        } ?: logTimeout(UNREGISTER)
    }

    suspend fun deleteLocalToken() {
        pushTokens.deleteToken()
    }

    private fun tokensToRegister(): Flow<String> =
        merge(
            flow { pushTokens.currentToken()?.let { token -> emit(token) } },
            pushTokens.changes,
        )

    private suspend fun register(token: String) {
        val args = PushTokensRegisterArgs(platform = pushTokens.platform.toModel(), token = token)
        withTimeoutOrNull(BACKEND_TIMEOUT) {
            mutate(REGISTER, ConvexJson.encodeArgs(PushTokensRegisterArgs.serializer(), args))
        } ?: logTimeout(REGISTER)
    }

    private suspend fun mutate(
        name: String,
        args: JsonObject,
    ) {
        backend
            .mutation(name, args, JsonElement.serializer())
            .onErr { error -> telemetry.log(TAG, "$name failed: $error", Severity.Warning) }
    }

    private fun logTimeout(name: String) {
        telemetry.log(TAG, "$name timed out", Severity.Warning)
    }

    private fun PushPlatform.toModel(): PushTokensRegisterPlatform =
        when (this) {
            PushPlatform.Android -> PushTokensRegisterPlatform.Android
            PushPlatform.Ios -> PushTokensRegisterPlatform.Ios
        }

    private companion object {
        const val TAG = "PushTokenRegistration"
        const val REGISTER = "pushTokens:register"
        const val UNREGISTER = "pushTokens:unregister"

        // The Convex client queues calls while it has no connection instead of failing them.
        val BACKEND_TIMEOUT = 20.seconds
        val UNREGISTER_TIMEOUT = 5.seconds
    }
}
