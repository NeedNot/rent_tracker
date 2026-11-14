package net.neednot.renttracker.feature.payments.data

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
import net.neednot.renttracker.feature.payments.domain.PaymentError
import kotlin.time.Duration.Companion.seconds

internal class PaymentBackendCalls(
    private val backend: Backend,
    private val telemetry: Telemetry,
) {
    fun <A, T> subscribe(
        name: String,
        args: A,
        argsSerializer: KSerializer<A>,
        resultSerializer: KSerializer<T>,
    ): Flow<Result<T, PaymentError>> =
        backend
            .subscribe(name, ConvexJson.encodeArgs(argsSerializer, args), resultSerializer)
            .map { result -> result.toPaymentResult() }

    suspend fun <A, T> mutate(
        name: String,
        args: A,
        argsSerializer: KSerializer<A>,
        resultSerializer: KSerializer<T>,
    ): Result<T, PaymentError> {
        val answer =
            withTimeoutOrNull(BACKEND_TIMEOUT) {
                backend.mutation(name, ConvexJson.encodeArgs(argsSerializer, args), resultSerializer)
            } ?: return Err(PaymentError.Offline)
        return answer.toPaymentResult()
    }

    suspend fun <A> mutateWithoutAnswer(
        name: String,
        args: A,
        argsSerializer: KSerializer<A>,
    ): Result<Unit, PaymentError> = mutate(name, args, argsSerializer, JsonElement.serializer()).map { }

    private fun <T> Result<T, BackendError>.toPaymentResult(): Result<T, PaymentError> =
        mapError { it.toPaymentError() }.onErr { error ->
            if (error is PaymentError.Unexpected) telemetry.log(TAG, error.message, Severity.Error)
        }

    private companion object {
        const val TAG = "PaymentRepository"

        // The Convex client queues calls while it has no connection instead of failing them.
        val BACKEND_TIMEOUT = 20.seconds
    }
}
