package net.neednot.renttracker.core.backend

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonObject

class NotYetSupportedBackend : Backend {
    override val authState: StateFlow<BackendAuthState> =
        MutableStateFlow<BackendAuthState>(BackendAuthState.SignedOut).asStateFlow()

    override fun <T> subscribe(
        name: String,
        args: JsonObject,
        resultSerializer: KSerializer<T>,
    ): Flow<Result<T, BackendError>> = flowOf(notYetSupported())

    override suspend fun <T> mutation(
        name: String,
        args: JsonObject,
        resultSerializer: KSerializer<T>,
    ): Result<T, BackendError> = notYetSupported()

    override suspend fun <T> action(
        name: String,
        args: JsonObject,
        resultSerializer: KSerializer<T>,
    ): Result<T, BackendError> = notYetSupported()

    private fun notYetSupported(): Result<Nothing, BackendError> =
        Err(BackendError.Internal("The Convex backend is not supported on iOS yet"))
}
