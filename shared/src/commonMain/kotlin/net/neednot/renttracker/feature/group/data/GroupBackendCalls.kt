package net.neednot.renttracker.feature.group.data

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onErr
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement
import net.neednot.renttracker.core.backend.Backend
import net.neednot.renttracker.core.backend.ConvexJson
import net.neednot.renttracker.core.telemetry.Severity
import net.neednot.renttracker.core.telemetry.Telemetry
import net.neednot.renttracker.feature.group.domain.GroupError
import kotlin.time.Duration.Companion.seconds

internal class GroupBackendCalls(
    private val backend: Backend,
    private val telemetry: Telemetry,
) {
    suspend fun <A> mutate(
        name: String,
        args: A,
        argsSerializer: KSerializer<A>,
    ): Result<Unit, GroupError> {
        val answer =
            withTimeoutOrNull(BACKEND_TIMEOUT) {
                backend.mutation(name, ConvexJson.encodeArgs(argsSerializer, args), JsonElement.serializer())
            } ?: return Err(GroupError.Offline)
        return answer.map { }.mapError { it.toGroupError() }.onErr { error ->
            if (error is GroupError.Unexpected) telemetry.log(TAG, error.message, Severity.Error)
        }
    }

    private companion object {
        const val TAG = "GroupRepository"

        // The Convex client queues calls while it has no connection instead of failing them.
        val BACKEND_TIMEOUT = 20.seconds
    }
}
