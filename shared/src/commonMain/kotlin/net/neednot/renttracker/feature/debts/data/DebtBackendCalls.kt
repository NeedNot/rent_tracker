package net.neednot.renttracker.feature.debts.data

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onErr
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement
import net.neednot.renttracker.core.backend.Backend
import net.neednot.renttracker.core.backend.BackendError
import net.neednot.renttracker.core.backend.ConvexJson
import net.neednot.renttracker.core.telemetry.Severity
import net.neednot.renttracker.core.telemetry.Telemetry
import net.neednot.renttracker.feature.debts.domain.DebtError
import kotlin.time.Duration.Companion.seconds

internal class DebtBackendCalls(
    private val backend: Backend,
    private val telemetry: Telemetry,
) {
    fun <A, T> subscribe(
        name: String,
        args: A,
        argsSerializer: KSerializer<A>,
        resultSerializer: KSerializer<T>,
    ): Flow<Result<T, DebtError>> =
        backend
            .subscribe(name, ConvexJson.encodeArgs(argsSerializer, args), resultSerializer)
            .map { result -> result.toDebtResult() }

    suspend fun <A, T> mutate(
        name: String,
        args: A,
        argsSerializer: KSerializer<A>,
        resultSerializer: KSerializer<T>,
    ): Result<T, DebtError> {
        val answer =
            withTimeoutOrNull(BACKEND_TIMEOUT) {
                backend.mutation(name, ConvexJson.encodeArgs(argsSerializer, args), resultSerializer)
            } ?: return Err(DebtError.Offline)
        return answer.toDebtResult()
    }

    suspend fun <A> mutateWithoutAnswer(
        name: String,
        args: A,
        argsSerializer: KSerializer<A>,
    ): Result<Unit, DebtError> = mutate(name, args, argsSerializer, JsonElement.serializer()).map { }

    private fun <T> Result<T, BackendError>.toDebtResult(): Result<T, DebtError> =
        mapError { it.toDebtError() }.onErr { error ->
            if (error is DebtError.Unexpected) telemetry.log(TAG, error.message, Severity.Error)
        }

    private companion object {
        const val TAG = "DebtRepository"

        // The Convex client queues calls while it has no connection instead of failing them.
        val BACKEND_TIMEOUT = 20.seconds
    }
}
