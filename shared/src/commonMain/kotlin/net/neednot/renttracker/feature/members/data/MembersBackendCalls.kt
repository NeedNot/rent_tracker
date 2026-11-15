package net.neednot.renttracker.feature.members.data

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onErr
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement
import net.neednot.renttracker.core.backend.Backend
import net.neednot.renttracker.core.backend.BackendError
import net.neednot.renttracker.core.backend.ConvexJson
import net.neednot.renttracker.core.telemetry.Severity
import net.neednot.renttracker.core.telemetry.Telemetry
import net.neednot.renttracker.feature.members.domain.MembersError
import kotlin.time.Duration.Companion.seconds

internal class MembersBackendCalls(
    private val backend: Backend,
    private val telemetry: Telemetry,
) {
    suspend fun <A> mutate(
        name: String,
        args: A,
        argsSerializer: KSerializer<A>,
    ): Result<Unit, MembersError> {
        val answer =
            withTimeoutOrNull(BACKEND_TIMEOUT) {
                backend.mutation(name, ConvexJson.encodeArgs(argsSerializer, args), JsonElement.serializer())
            } ?: return Err(MembersError.Offline)
        return membersResultOf(answer).map { }
    }

    fun <T> membersResultOf(result: Result<T, BackendError>): Result<T, MembersError> =
        result.mapError { it.toMembersError() }.onErr { error ->
            if (error is MembersError.Unexpected) telemetry.log(TAG, error.message, Severity.Error)
        }

    private companion object {
        const val TAG = "MembersRepository"

        // The Convex client queues calls while it has no connection instead of failing them.
        val BACKEND_TIMEOUT = 20.seconds
    }
}
