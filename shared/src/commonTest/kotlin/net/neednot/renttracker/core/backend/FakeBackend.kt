package net.neednot.renttracker.core.backend

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

class FakeBackend : Backend {
    override val authState = MutableStateFlow<BackendAuthState>(BackendAuthState.SignedIn)

    val mutationAnswers = mutableMapOf<String, Result<JsonElement, BackendError>>()
    val mutations = mutableListOf<Pair<String, JsonObject>>()

    override fun <T> subscribe(
        name: String,
        args: JsonObject,
        resultSerializer: KSerializer<T>,
    ): Flow<Result<T, BackendError>> = emptyFlow()

    override suspend fun <T> mutation(
        name: String,
        args: JsonObject,
        resultSerializer: KSerializer<T>,
    ): Result<T, BackendError> {
        mutations += name to args
        val answer = mutationAnswers[name] ?: return Err(BackendError.Internal("No answer set for $name"))
        return answer.map { ConvexJson.decode(resultSerializer, it) }
    }

    override suspend fun <T> action(
        name: String,
        args: JsonObject,
        resultSerializer: KSerializer<T>,
    ): Result<T, BackendError> = Err(BackendError.Internal("No action answers in this fake"))
}
