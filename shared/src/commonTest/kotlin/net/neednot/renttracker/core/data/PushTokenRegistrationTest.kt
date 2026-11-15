package net.neednot.renttracker.core.data

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.neednot.renttracker.core.backend.BackendAuthState
import net.neednot.renttracker.core.backend.BackendError
import net.neednot.renttracker.core.backend.FakeBackend
import net.neednot.renttracker.core.telemetry.RecordingTelemetry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class PushTokenRegistrationTest {
    private val backend = FakeBackend().apply { mutationAnswers[REGISTER] = Ok(JsonNull) }
    private val pushTokens = FakePushTokens()
    private val telemetry = RecordingTelemetry()
    private val registration = PushTokenRegistration(backend, pushTokens, telemetry)

    private fun registered(token: String) =
        REGISTER to
            buildJsonObject {
                put("platform", "android")
                put("token", token)
            }

    @Test
    fun registersThePhonesTokenOnceSignedIn() =
        runTest {
            val job = launch { registration.registerWhileSignedIn() }
            runCurrent()

            assertEquals(listOf(registered("phone-token")), backend.mutations)
            job.cancel()
        }

    @Test
    fun waitsForSignInBeforeRegistering() =
        runTest {
            backend.authState.value = BackendAuthState.SignedOut
            val job = launch { registration.registerWhileSignedIn() }
            runCurrent()
            assertEquals(emptyList(), backend.mutations)

            backend.authState.value = BackendAuthState.SignedIn
            runCurrent()

            assertEquals(listOf(registered("phone-token")), backend.mutations)
            job.cancel()
        }

    @Test
    fun registersANewTokenWhenTheOldOneIsReplaced() =
        runTest {
            val job = launch { registration.registerWhileSignedIn() }
            runCurrent()

            pushTokens.changes.emit("new-token")
            runCurrent()

            assertEquals(listOf(registered("phone-token"), registered("new-token")), backend.mutations)
            job.cancel()
        }

    @Test
    fun registersNothingWhenPushIsUnavailable() =
        runTest {
            pushTokens.token = null
            val job = launch { registration.registerWhileSignedIn() }
            runCurrent()

            assertEquals(emptyList(), backend.mutations)
            job.cancel()
        }

    @Test
    fun aRefusedRegistrationIsLoggedAndTheNextTokenStillRegisters() =
        runTest {
            backend.mutationAnswers[REGISTER] = Err(BackendError.Server("down"))
            val job = launch { registration.registerWhileSignedIn() }
            runCurrent()

            pushTokens.changes.emit("new-token")
            runCurrent()

            assertEquals(2, backend.mutations.size)
            assertEquals(2, telemetry.logs.size)
            job.cancel()
        }

    @Test
    fun aRegistrationWithoutAConnectionGivesUpAndIsLogged() =
        runTest {
            backend.isReachable = false
            val job = launch { registration.registerWhileSignedIn() }

            advanceTimeBy(21.seconds)

            assertEquals(listOf("pushTokens:register timed out"), telemetry.logs)
            job.cancel()
        }

    @Test
    fun unregisteringSendsThePhonesCurrentToken() =
        runTest {
            backend.mutationAnswers[UNREGISTER] = Ok(JsonNull)

            registration.unregisterCurrentToken()

            assertEquals(listOf(UNREGISTER to buildJsonObject { put("token", "phone-token") }), backend.mutations)
        }

    @Test
    fun unregisteringWithoutAConnectionGivesUpQuickly() =
        runTest {
            backend.isReachable = false

            registration.unregisterCurrentToken()

            assertEquals(listOf("pushTokens:unregister timed out"), telemetry.logs)
        }

    private companion object {
        const val REGISTER = "pushTokens:register"
        const val UNREGISTER = "pushTokens:unregister"
    }
}
