package net.neednot.renttracker.feature.auth.data

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.neednot.renttracker.core.auth.AuthSessionError
import net.neednot.renttracker.core.auth.AuthSessionState
import net.neednot.renttracker.core.auth.FakeAuthSession
import net.neednot.renttracker.core.auth.FakeGoogleIdTokenProvider
import net.neednot.renttracker.core.auth.GoogleIdToken
import net.neednot.renttracker.core.auth.GoogleIdTokenError
import net.neednot.renttracker.core.backend.BackendAuthState
import net.neednot.renttracker.core.backend.BackendError
import net.neednot.renttracker.core.backend.FakeBackend
import net.neednot.renttracker.core.telemetry.RecordingTelemetry
import net.neednot.renttracker.feature.auth.domain.AuthError
import net.neednot.renttracker.feature.auth.domain.SignedInAccount
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthRepositoryImplTest {
    private val authSession = FakeAuthSession()
    private val googleIdTokenProvider = FakeGoogleIdTokenProvider()
    private val backend = FakeBackend()
    private val telemetry = RecordingTelemetry()
    private val repository = AuthRepositoryImpl(authSession, googleIdTokenProvider, backend, telemetry)

    @Test
    fun sendingACodeAsksTheAuthServer() =
        runTest {
            val result = repository.sendSignInCode("dalton.h@gmail.com")

            assertEquals(Ok(Unit), result)
            assertEquals(listOf("dalton.h@gmail.com"), authSession.sentCodesTo)
        }

    @Test
    fun sendingACodeTooSoonReportsHowLongToWait() =
        runTest {
            authSession.sendSignInCodeResult = Err(AuthSessionError.RateLimited(retryAfterSeconds = 37))

            assertEquals(Err(AuthError.RateLimited(retryAfterSeconds = 37)), repository.sendSignInCode("a@b.co"))
        }

    @Test
    fun firstSignInWithACodeEnsuresTheAccountAndReportsItAsNew() =
        runTest {
            backend.answerEnsure(created = true)

            val result = repository.signInWithCode("dalton.h@gmail.com", "123456")

            assertEquals(Ok(SignedInAccount(displayName = "dalton.h", isNew = true)), result)
            assertEquals(listOf("dalton.h@gmail.com" to "123456"), authSession.codeSignIns)
            assertEquals(listOf(ENSURE), backend.mutations.map { it.first })
        }

    @Test
    fun laterSignInReportsAnExistingAccount() =
        runTest {
            backend.answerEnsure(created = false)

            val result = repository.signInWithCode("dalton.h@gmail.com", "123456")

            assertEquals(Ok(SignedInAccount(displayName = "dalton.h", isNew = false)), result)
        }

    @Test
    fun wrongCodeNeverReachesTheBackend() =
        runTest {
            authSession.signInResult = Err(AuthSessionError.InvalidCode)

            val result = repository.signInWithCode("dalton.h@gmail.com", "000000")

            assertEquals(Err(AuthError.InvalidCode), result)
            assertTrue(backend.mutations.isEmpty())
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun ensureWaitsUntilConvexHasAcceptedTheSession() =
        runTest {
            backend.answerEnsure(created = false)
            backend.authState.value = BackendAuthState.Loading

            val signIn = async { repository.signInWithCode("dalton.h@gmail.com", "123456") }
            runCurrent()
            val mutationsWhileLoading = backend.mutations.toList()
            backend.authState.value = BackendAuthState.SignedIn

            assertTrue(mutationsWhileLoading.isEmpty())
            assertTrue(signIn.await().isOk)
            assertEquals(0, authSession.signOutCount)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun signInThatCannotReachTheAccountGivesUpAndSignsOutAgain() =
        runTest {
            backend.authState.value = BackendAuthState.Loading

            val result = repository.signInWithCode("dalton.h@gmail.com", "123456")

            assertEquals(Err(AuthError.Offline), result)
            assertEquals(BACKEND_TIMEOUT_MILLIS, currentTime)
            assertEquals(1, authSession.signOutCount)
            assertEquals(AuthSessionState.SignedOut, authSession.state.value)
        }

    @Test
    fun failedEnsureSignsOutAndIsLogged() =
        runTest {
            backend.mutationAnswers[ENSURE] = Err(BackendError.Server("down"))

            val result = repository.signInWithCode("dalton.h@gmail.com", "123456")

            assertEquals(Err(AuthError.Unexpected("down")), result)
            assertEquals(1, authSession.signOutCount)
            assertEquals(listOf("down"), telemetry.logs)
        }

    @Test
    fun googleSignInSendsTheIdTokenAndNonceThenEnsuresTheAccount() =
        runTest {
            backend.answerEnsure(created = true)
            val token = GoogleIdToken(idToken = "id-token", nonce = "nonce-1")
            googleIdTokenProvider.result = Ok(token)

            val result = repository.signInWithGoogle()

            assertEquals(Ok(SignedInAccount(displayName = "dalton.h", isNew = true)), result)
            assertEquals(listOf(token), authSession.googleSignIns)
            assertEquals(listOf(ENSURE), backend.mutations.map { it.first })
        }

    @Test
    fun cancellingGoogleSignInTouchesNothing() =
        runTest {
            googleIdTokenProvider.result = Err(GoogleIdTokenError.Cancelled)

            val result = repository.signInWithGoogle()

            assertEquals(Err(AuthError.GoogleSignInCancelled), result)
            assertTrue(authSession.googleSignIns.isEmpty())
            assertTrue(backend.mutations.isEmpty())
        }

    @Test
    fun googleTokenRejectedByTheAuthServerIsAGoogleFailure() =
        runTest {
            authSession.signInResult = Err(AuthSessionError.Server(401, "INVALID_TOKEN", null))

            assertEquals(Err(AuthError.GoogleSignInFailed), repository.signInWithGoogle())
        }

    @Test
    fun displayNameIsTrimmedSavedAndReturned() =
        runTest {
            backend.mutationAnswers[SET_DISPLAY_NAME] = Ok(JsonPrimitive("Dalton"))

            val result = repository.setDisplayName("  Dalton ")

            assertEquals(Ok("Dalton"), result)
            assertEquals(
                listOf(SET_DISPLAY_NAME to buildJsonObject { put("displayName", "Dalton") }),
                backend.mutations,
            )
        }

    @Test
    fun displayNameRejectedByTheServerIsInvalid() =
        runTest {
            backend.mutationAnswers[SET_DISPLAY_NAME] =
                Err(
                    BackendError.Convex(
                        "ValidationFailed",
                        "Too long",
                        buildJsonObject { put("field", "displayName") },
                    ),
                )

            assertEquals(Err(AuthError.InvalidDisplayName), repository.setDisplayName("x"))
        }

    @Test
    fun signOutClearsTheSession() =
        runTest {
            authSession.state.value = AuthSessionState.SignedIn(FakeAuthSession.USER_ID)

            val result = repository.signOut()

            assertEquals(Ok(Unit), result)
            assertEquals(AuthSessionState.SignedOut, repository.sessionState.value)
        }

    @Test
    fun signOutThatCannotClearStorageIsReported() =
        runTest {
            authSession.signOutResult = Err(AuthSessionError.Storage)

            assertTrue(repository.signOut().isErr)
            assertEquals(1, telemetry.logs.size)
        }

    private fun FakeBackend.answerEnsure(created: Boolean) {
        mutationAnswers[ENSURE] =
            Ok(
                buildJsonObject {
                    put("accountId", "account-1")
                    put("created", created)
                    put("displayName", "dalton.h")
                    put("email", "dalton.h@gmail.com")
                },
            )
    }

    private companion object {
        const val ENSURE = "accounts:ensure"
        const val SET_DISPLAY_NAME = "accounts:setDisplayName"
        const val BACKEND_TIMEOUT_MILLIS = 20_000L
    }
}
