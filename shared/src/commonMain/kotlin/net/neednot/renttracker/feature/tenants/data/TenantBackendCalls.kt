package net.neednot.renttracker.feature.tenants.data

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onErr
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement
import net.neednot.renttracker.core.backend.Backend
import net.neednot.renttracker.core.backend.BackendError
import net.neednot.renttracker.core.backend.ConvexJson
import net.neednot.renttracker.core.telemetry.Severity
import net.neednot.renttracker.core.telemetry.Telemetry
import net.neednot.renttracker.feature.tenants.domain.TenantError
import kotlin.time.Duration.Companion.seconds

internal class TenantBackendCalls(
    private val backend: Backend,
    private val telemetry: Telemetry,
) {
    fun <A, T> subscribe(
        name: String,
        args: A,
        argsSerializer: KSerializer<A>,
        resultSerializer: KSerializer<T>,
    ): Flow<Result<T, TenantError>> =
        backend
            .subscribe(name, ConvexJson.encodeArgs(argsSerializer, args), resultSerializer)
            .map { it.toTenantResult() }

    suspend fun <A, T> queryOnce(
        name: String,
        args: A,
        argsSerializer: KSerializer<A>,
        resultSerializer: KSerializer<T>,
    ): Result<T, TenantError> =
        withTimeoutOrNull(BACKEND_TIMEOUT) { subscribe(name, args, argsSerializer, resultSerializer).first() }
            ?: Err(TenantError.Offline)

    suspend fun <A, T> mutate(
        name: String,
        args: A,
        argsSerializer: KSerializer<A>,
        resultSerializer: KSerializer<T>,
    ): Result<T, TenantError> {
        val answer =
            withTimeoutOrNull(BACKEND_TIMEOUT) {
                backend.mutation(name, ConvexJson.encodeArgs(argsSerializer, args), resultSerializer)
            } ?: return Err(TenantError.Offline)
        return answer.toTenantResult()
    }

    suspend fun <A> mutateWithoutAnswer(
        name: String,
        args: A,
        argsSerializer: KSerializer<A>,
    ): Result<Unit, TenantError> = mutate(name, args, argsSerializer, JsonElement.serializer()).map { }

    private fun <T> Result<T, BackendError>.toTenantResult(): Result<T, TenantError> =
        mapError { it.toTenantError() }.onErr { error ->
            if (error is TenantError.Unexpected) telemetry.log(TAG, error.message, Severity.Error)
        }

    private companion object {
        const val TAG = "TenantRepository"

        // The Convex client queues calls while it has no connection instead of failing them.
        val BACKEND_TIMEOUT = 20.seconds
    }
}
