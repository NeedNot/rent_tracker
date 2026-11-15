package net.neednot.renttracker.feature.account.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cash.turbine.test
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.neednot.renttracker.core.auth.AuthSessionError
import net.neednot.renttracker.core.auth.FakeAuthSession
import net.neednot.renttracker.core.backend.BackendError
import net.neednot.renttracker.core.backend.FakeBackend
import net.neednot.renttracker.core.data.AccountProfileEntity
import net.neednot.renttracker.core.data.AccountProfileSync
import net.neednot.renttracker.core.data.PendingPaymentQueue
import net.neednot.renttracker.core.data.PendingPaymentStatus
import net.neednot.renttracker.core.data.RecordingSyncScheduler
import net.neednot.renttracker.core.data.RentTrackerDatabase
import net.neednot.renttracker.core.data.ScreenResultStore
import net.neednot.renttracker.core.data.pendingPaymentEntity
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.telemetry.NoOpTelemetry
import net.neednot.renttracker.feature.account.domain.AccountError
import net.neednot.renttracker.feature.account.domain.AccountProfile
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class AccountRepositoryImplTest {
    private val database =
        Room
            .inMemoryDatabaseBuilder<RentTrackerDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()

    private val backend = FakeBackend()
    private val authSession = FakeAuthSession()

    private val repository =
        AccountRepositoryImpl(
            backend = backend,
            authSession = authSession,
            accountProfileSync = AccountProfileSync(backend, database, ScreenResultStore(database)),
            pendingPaymentQueue = PendingPaymentQueue(database, RecordingSyncScheduler()),
            telemetry = NoOpTelemetry(),
        )

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    private fun answerMe(
        accountId: String = "me",
        displayName: String = "Sam Rivera",
        deletionRequestedAt: Long? = null,
    ) {
        backend.answerSubscription(
            ME,
            Ok(
                buildJsonObject {
                    put("accountId", accountId)
                    put("displayName", displayName)
                    put("email", "sam@example.com")
                    deletionRequestedAt?.let { put("deletionRequestedAt", it) }
                },
            ),
        )
    }

    @Test
    fun theProfileStartsEmptyWhileTheServerIsAnswering() =
        runTest {
            repository.observeProfile().test {
                val item = awaitItem()
                assertEquals(null, item.value)
                assertEquals(SyncStatus.Syncing, item.status)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun aMeResultIsCachedAndShown() =
        runTest {
            repository.observeProfile().test {
                skipItems(1)

                answerMe(deletionRequestedAt = DELETION_REQUESTED_AT)

                val item = awaitItem()
                assertEquals(
                    AccountProfile(
                        "Sam Rivera",
                        "sam@example.com",
                        Instant.fromEpochMilliseconds(DELETION_REQUESTED_AT),
                    ),
                    item.value,
                )
                assertEquals(SyncStatus.Synced, item.status)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun aMeResultReplacesTheCachedRow() =
        runTest {
            database.accountProfileDao().upsert(AccountProfileEntity("old", "Old Name", "old@example.com", null))
            repository.observeProfile().test {
                skipItems(1)

                answerMe(accountId = "me", displayName = "Sam Rivera")

                awaitItem()
                assertEquals(
                    listOf(AccountProfileEntity("me", "Sam Rivera", "sam@example.com", null)),
                    allProfiles(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun aFailedSubscriptionKeepsTheCachedProfileAndReportsTheError() =
        runTest {
            database.accountProfileDao().upsert(AccountProfileEntity("me", "Sam Rivera", "sam@example.com", null))
            repository.observeProfile().test {
                skipItems(1)

                backend.answerSubscription(ME, Err(BackendError.Convex("NotSignedIn", MESSAGE, null)))

                val item = awaitItem()
                assertEquals("Sam Rivera", item.value?.displayName)
                assertEquals(SyncStatus.Failed(AccountError.NotSignedIn), item.status)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun editingTheNameSendsTheTrimmedName() =
        runTest {
            backend.mutationAnswers[SET_DISPLAY_NAME] = Ok(Json.parseToJsonElement("\"Sam R\""))

            assertEquals(Ok(Unit), repository.editName("  Sam R  "))

            assertEquals(
                listOf(SET_DISPLAY_NAME to buildJsonObject { put("displayName", "Sam R") }),
                backend.mutations,
            )
        }

    @Test
    fun aRefusedNameKeepsTheServerMessageAndField() =
        runTest {
            val details = buildJsonObject { put("field", "displayName") }
            backend.mutationAnswers[SET_DISPLAY_NAME] = Err(BackendError.Convex("ValidationFailed", MESSAGE, details))

            assertEquals(
                Err(AccountError.ValidationFailed(MESSAGE, field = "displayName")),
                repository.editName("Sam"),
            )
        }

    @Test
    fun requestingDeletionCallsTheServerWithoutArguments() =
        runTest {
            backend.mutationAnswers[REQUEST_DELETION] = Ok(Json.parseToJsonElement("$DELETION_REQUESTED_AT"))

            assertEquals(Ok(Unit), repository.requestDeletion())

            assertEquals(listOf(REQUEST_DELETION to buildJsonObject {}), backend.mutations)
        }

    @Test
    fun aDeletionRefusedBecauseOfOwnedGroupsKeepsTheServerMessage() =
        runTest {
            val message = "Transfer ownership of 1st Street first."
            backend.mutationAnswers[REQUEST_DELETION] = Err(BackendError.Convex("ValidationFailed", message, null))

            assertEquals(Err(AccountError.ValidationFailed(message)), repository.requestDeletion())
        }

    @Test
    fun serverErrorCodesBecomeAccountErrors() =
        runTest {
            mapOf(
                "NotSignedIn" to AccountError.NotSignedIn,
                "Weird" to AccountError.Unexpected("Weird: $MESSAGE"),
            ).forEach { (code, expected) ->
                backend.mutationAnswers[REQUEST_DELETION] = Err(BackendError.Convex(code, MESSAGE, null))

                assertEquals(Err(expected), repository.requestDeletion(), "code = $code")
            }
        }

    @Test
    fun aChangeWithoutAConnectionReportsOffline() =
        runTest {
            backend.isReachable = false

            assertEquals(Err(AccountError.Offline), repository.editName("Sam"))
        }

    @Test
    fun signingOutEndsTheSession() =
        runTest {
            assertEquals(Ok(Unit), repository.signOut())

            assertEquals(1, authSession.signOutCount)
        }

    @Test
    fun signingOutClearsTheCachedProfile() =
        runTest {
            database.accountProfileDao().upsert(AccountProfileEntity("me", "Sam Rivera", "sam@example.com", null))

            repository.signOut()

            assertEquals(emptyList(), allProfiles())
        }

    @Test
    fun signingOutDiscardsPaymentsThatHaveNotSynced() =
        runTest {
            database.pendingPaymentDao().insert(pendingPaymentEntity("c1", "t1"))

            repository.signOut()

            assertEquals(0, repository.observeUnsyncedPaymentCount().first())
        }

    @Test
    fun countsEveryPaymentThatHasNotSyncedWhateverItsStatus() =
        runTest {
            database.pendingPaymentDao().insert(pendingPaymentEntity("c1", "t1"))
            database.pendingPaymentDao().insert(
                pendingPaymentEntity("c2", "t1", sequence = 2).copy(status = PendingPaymentStatus.Rejected),
            )

            assertEquals(2, repository.observeUnsyncedPaymentCount().first())
        }

    @Test
    fun aFailedSignOutKeepsTheCachedProfile() =
        runTest {
            database.accountProfileDao().upsert(AccountProfileEntity("me", "Sam Rivera", "sam@example.com", null))
            authSession.signOutResult = Err(AuthSessionError.Network)

            repository.signOut()

            assertEquals(1, allProfiles().size)
        }

    @Test
    fun aSignOutWithoutAConnectionReportsOffline() =
        runTest {
            authSession.signOutResult = Err(AuthSessionError.Network)

            assertEquals(Err(AccountError.Offline), repository.signOut())
        }

    private suspend fun allProfiles(): List<AccountProfileEntity> =
        listOfNotNull(database.accountProfileDao().observe().first())

    private companion object {
        const val ME = "accounts:me"
        const val SET_DISPLAY_NAME = "accounts:setDisplayName"
        const val REQUEST_DELETION = "accounts:requestDeletion"
        const val MESSAGE = "Something is wrong."
        const val DELETION_REQUESTED_AT = 1_794_634_333_900L
    }
}
