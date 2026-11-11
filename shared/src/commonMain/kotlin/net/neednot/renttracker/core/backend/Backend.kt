package net.neednot.renttracker.core.backend

import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonObject

interface Backend {
    val authState: StateFlow<BackendAuthState>

    fun <T> subscribe(
        name: String,
        args: JsonObject,
        resultSerializer: KSerializer<T>,
    ): Flow<Result<T, BackendError>>

    suspend fun <T> mutation(
        name: String,
        args: JsonObject,
        resultSerializer: KSerializer<T>,
    ): Result<T, BackendError>

    suspend fun <T> action(
        name: String,
        args: JsonObject,
        resultSerializer: KSerializer<T>,
    ): Result<T, BackendError>
}
