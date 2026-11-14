package net.neednot.renttracker.core.backend

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

class FakeBackend : Backend {
    override val authState = MutableStateFlow<BackendAuthState>(BackendAuthState.SignedIn)

    var isReachable = true

    val mutationAnswers = mutableMapOf<String, Result<JsonElement, BackendError>>()
    val mutations = mutableListOf<Pair<String, JsonObject>>()

    val subscriptions = mutableListOf<Pair<String, JsonObject>>()
    var openSubscriptionCount = 0
        private set

    private val subscriptionAnswers = mutableMapOf<String, MutableStateFlow<Result<JsonElement, BackendError>?>>()

    fun answerSubscription(
        name: String,
        answer: Result<JsonElement, BackendError>,
    ) {
        subscriptionAnswersTo(name).value = answer
    }

    override fun <T> subscribe(
        name: String,
        args: JsonObject,
        resultSerializer: KSerializer<T>,
    ): Flow<Result<T, BackendError>> =
        flow {
            subscriptions += name to args
            openSubscriptionCount++
            try {
                emitAll(
                    subscriptionAnswersTo(name)
                        .filterNotNull()
                        .map { answer -> answer.map { ConvexJson.decode(resultSerializer, it) } },
                )
            } finally {
                openSubscriptionCount--
            }
        }

    override suspend fun <T> mutation(
        name: String,
        args: JsonObject,
        resultSerializer: KSerializer<T>,
    ): Result<T, BackendError> {
        mutations += name to args
        if (!isReachable) awaitCancellation()
        val answer = mutationAnswers[name] ?: return Err(BackendError.Internal("No answer set for $name"))
        return answer.map { ConvexJson.decode(resultSerializer, it) }
    }

    override suspend fun <T> action(
        name: String,
        args: JsonObject,
        resultSerializer: KSerializer<T>,
    ): Result<T, BackendError> = Err(BackendError.Internal("No action answers in this fake"))

    private fun subscriptionAnswersTo(name: String) = subscriptionAnswers.getOrPut(name) { MutableStateFlow(null) }
}
