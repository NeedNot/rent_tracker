package net.neednot.renttracker.backend

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import dev.convex.android.ConvexClient
import dev.convex.android.ConvexError
import dev.convex.android.InternalError
import dev.convex.android.ServerError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import net.neednot.renttracker.core.backend.Backend
import net.neednot.renttracker.core.backend.BackendAuthState
import net.neednot.renttracker.core.backend.BackendError
import net.neednot.renttracker.core.backend.ConvexJson

class ConvexBackend(
    deploymentUrl: String,
) : Backend {
    init {
        require(deploymentUrl.isNotBlank()) { "convex.url is missing from local.properties" }
    }

    private val client = ConvexClient(deploymentUrl)

    // Stays signed out until sign-in replaces ConvexClient with ConvexClientWithAuth.
    override val authState: StateFlow<BackendAuthState> =
        MutableStateFlow<BackendAuthState>(BackendAuthState.SignedOut).asStateFlow()

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
}
