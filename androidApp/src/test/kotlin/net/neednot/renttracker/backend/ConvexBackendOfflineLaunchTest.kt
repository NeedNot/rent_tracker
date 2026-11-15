package net.neednot.renttracker.backend

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import net.neednot.renttracker.core.backend.BackendAuthState
import net.neednot.renttracker.core.telemetry.NoOpTelemetry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ConvexBackendOfflineLaunchTest {
    private val nativeClient = UnreachableConvexClient()
    private val uncaught = mutableListOf<Throwable>()
    private val previousHandler = Thread.getDefaultUncaughtExceptionHandler()

    @Before
    fun recordCrashes() {
        Thread.setDefaultUncaughtExceptionHandler { _, error -> synchronized(uncaught) { uncaught += error } }
    }

    @After
    fun restoreCrashHandler() {
        Thread.setDefaultUncaughtExceptionHandler(previousHandler)
    }

    @Test
    fun aSubscriptionOpenedBeforeConvexWasEverReachedWaitsInsteadOfFailing() =
        runBlocking {
            val backend = newBackend()
            val firstResult =
                async(Dispatchers.Default) {
                    backend.subscribe(QUERY, JsonObject(emptyMap()), JsonElement.serializer()).first()
                }

            withContext(Dispatchers.Default) { Thread.sleep(UNREACHABLE_MILLIS) }
            nativeClient.isReachable = true
            withTimeout(RETRY_WAIT) {
                while (nativeClient.subscriptions.isEmpty()) withContext(Dispatchers.Default) { Thread.sleep(POLL) }
            }
            nativeClient.subscriptions.values
                .single()
                .onUpdate("{\"ok\":true}")

            assertTrue(withTimeout(RETRY_WAIT) { firstResult.await() }.isOk)
            assertEquals(emptyList<Throwable>(), uncaught)
        }

    @Test
    fun aSignedInLaunchWithoutAConnectionSignsConvexInOnceItIsReached() =
        runBlocking {
            val backend = newBackend()

            withContext(Dispatchers.Default) { Thread.sleep(UNREACHABLE_MILLIS) }
            nativeClient.isReachable = true

            val signedIn = withTimeout(RETRY_WAIT) { backend.authState.first { it == BackendAuthState.SignedIn } }
            assertEquals(BackendAuthState.SignedIn, signedIn)
            assertEquals(emptyList<Throwable>(), uncaught)
        }

    private fun newBackend() =
        ConvexBackend(
            deploymentUrl = "https://example.convex.cloud",
            context = ApplicationProvider.getApplicationContext(),
            authSession = SignedInAuthSession(),
            telemetry = NoOpTelemetry(),
            nativeClientFactory = { _, _, _ -> nativeClient },
        )

    private companion object {
        const val QUERY = "home:screen"
        const val UNREACHABLE_MILLIS = 500L
        const val POLL = 50L
        val RETRY_WAIT = 10.seconds
    }
}
