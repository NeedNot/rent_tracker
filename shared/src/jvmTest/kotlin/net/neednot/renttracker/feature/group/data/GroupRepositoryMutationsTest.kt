package net.neednot.renttracker.feature.group.data

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
import net.neednot.renttracker.core.backend.BackendError
import net.neednot.renttracker.core.backend.FakeBackend
import net.neednot.renttracker.core.data.FakeAsOfClock
import net.neednot.renttracker.core.data.GroupAccessWatcher
import net.neednot.renttracker.core.data.PaymentOrder
import net.neednot.renttracker.core.data.RentTrackerDatabase
import net.neednot.renttracker.core.data.ScreenResultStore
import net.neednot.renttracker.core.telemetry.NoOpTelemetry
import net.neednot.renttracker.feature.group.domain.GroupError
import net.neednot.renttracker.feature.group.domain.GroupSettings
import net.neednot.renttracker.model.groups.GroupsScreenResult
import net.neednot.renttracker.model.shared.Group
import net.neednot.renttracker.model.shared.GroupPaymentOrder
import net.neednot.renttracker.model.shared.GroupSummary
import net.neednot.renttracker.model.shared.Membership
import net.neednot.renttracker.model.shared.MembershipDebts
import net.neednot.renttracker.model.shared.MembershipMembers
import net.neednot.renttracker.model.shared.MembershipPayments
import net.neednot.renttracker.model.shared.MembershipRentTerms
import net.neednot.renttracker.model.shared.MembershipRole
import net.neednot.renttracker.model.shared.MembershipSettings
import net.neednot.renttracker.model.shared.MembershipTenants
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class GroupRepositoryMutationsTest {
    private val database =
        Room
            .inMemoryDatabaseBuilder<RentTrackerDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()

    private val backend = FakeBackend()
    private val screenResultStore = ScreenResultStore(database)
    private val groupAccessWatcher = GroupAccessWatcher(database, screenResultStore)

    private val repository =
        GroupRepositoryImpl(
            backend = backend,
            asOfClock = FakeAsOfClock(TEN_O_CLOCK),
            database = database,
            screenResultStore = screenResultStore,
            groupAccessWatcher = groupAccessWatcher,
            telemetry = NoOpTelemetry(),
        )

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun savingSettingsSendsEveryFieldAndTheExpectedVersion() =
        runTest {
            backend.mutationAnswers[UPDATE_SETTINGS] = Ok(JsonNull)
            val settings = GroupSettings("2nd Street", "America/Denver", PaymentOrder.ByDate, 10, 7_500)

            val result = repository.updateSettings(GROUP_ID, expectedVersion = 4, settings = settings)

            assertEquals(Ok(Unit), result)
            val expectedArgs =
                buildJsonObject {
                    put("expectedVersion", 4)
                    put("graceDays", 10)
                    put("groupId", GROUP_ID)
                    put("lateFeeFlatCents", 7_500)
                    put("name", "2nd Street")
                    put("paymentOrder", "byDate")
                    put("timeZone", "America/Denver")
                }
            assertEquals(listOf(UPDATE_SETTINGS to expectedArgs), backend.mutations)
        }

    @Test
    fun savingSettingsWithoutASuggestedFeeOmitsIt() =
        runTest {
            backend.mutationAnswers[UPDATE_SETTINGS] = Ok(JsonNull)

            repository.updateSettings(
                GROUP_ID,
                1,
                GroupSettings("1st Street", "America/Chicago", PaymentOrder.RentFirst, 5, null),
            )

            assertEquals(
                false,
                backend.mutations
                    .single()
                    .second
                    .containsKey("lateFeeFlatCents"),
            )
        }

    @Test
    fun archivingUnarchivingAndLeavingSendOnlyTheGroup() =
        runTest {
            backend.mutationAnswers[ARCHIVE] = Ok(JsonNull)
            backend.mutationAnswers[UNARCHIVE] = Ok(JsonNull)
            backend.mutationAnswers[LEAVE] = Ok(JsonNull)

            repository.archiveGroup(GROUP_ID)
            repository.unarchiveGroup(GROUP_ID)
            repository.leaveGroup(GROUP_ID)

            val onlyTheGroup = buildJsonObject { put("groupId", GROUP_ID) }
            assertEquals(
                listOf(ARCHIVE to onlyTheGroup, UNARCHIVE to onlyTheGroup, LEAVE to onlyTheGroup),
                backend.mutations,
            )
        }

    @Test
    fun aValidationErrorKeepsItsMessageAndField() =
        runTest {
            backend.mutationAnswers[UPDATE_SETTINGS] =
                Err(
                    BackendError.Convex(
                        code = "ValidationFailed",
                        message = "Grace period must be 0 to 15 whole days.",
                        details = buildJsonObject { put("field", "Grace period") },
                    ),
                )

            val result =
                repository.updateSettings(
                    GROUP_ID,
                    1,
                    GroupSettings("a", "America/Chicago", PaymentOrder.RentFirst, 99, null),
                )

            assertEquals(
                Err(GroupError.ValidationFailed("Grace period must be 0 to 15 whole days.", "Grace period")),
                result,
            )
        }

    @Test
    fun anEditConflictNamesWhoChangedTheSettings() =
        runTest {
            backend.mutationAnswers[UPDATE_SETTINGS] =
                Err(
                    BackendError.Convex(
                        code = "EditConflict",
                        message = "Sam Rivera changed this group while you were editing.",
                        details =
                            buildJsonObject {
                                put("currentVersion", 5)
                                put("lastEditedByName", "Sam Rivera")
                            },
                    ),
                )

            val result =
                repository.updateSettings(
                    GROUP_ID,
                    4,
                    GroupSettings("a", "America/Chicago", PaymentOrder.RentFirst, 5, null),
                )

            assertEquals(
                Err(GroupError.EditConflict("Sam Rivera changed this group while you were editing.", "Sam Rivera")),
                result,
            )
        }

    @Test
    fun anEditConflictWithoutDetailsHasNoEditorName() =
        runTest {
            backend.mutationAnswers[UPDATE_SETTINGS] =
                Err(BackendError.Convex("EditConflict", "This group changed while you were editing.", null))

            val result =
                repository.updateSettings(
                    GROUP_ID,
                    4,
                    GroupSettings("a", "America/Chicago", PaymentOrder.RentFirst, 5, null),
                )

            assertEquals(Err(GroupError.EditConflict("This group changed while you were editing.", null)), result)
        }

    @Test
    fun theOwnersRefusalToLeaveIsAValidationError() =
        runTest {
            backend.mutationAnswers[LEAVE] =
                Err(BackendError.Convex("ValidationFailed", "Transfer ownership before leaving.", null))

            assertEquals(
                Err(GroupError.ValidationFailed("Transfer ownership before leaving.")),
                repository.leaveGroup(GROUP_ID),
            )
        }

    @Test
    fun aDeniedMutationIsNoAccess() =
        runTest {
            backend.mutationAnswers[ARCHIVE] = Err(BackendError.Convex("PermissionDenied", "Only the owner.", null))

            assertEquals(Err(GroupError.NoAccess), repository.archiveGroup(GROUP_ID))
        }

    @Test
    fun aMutationWithoutAConnectionTimesOutAsOffline() =
        runTest {
            backend.isReachable = false

            assertEquals(Err(GroupError.Offline), repository.archiveGroup(GROUP_ID))
        }

    @Test
    fun theCachedGroupCarriesItsSettingsVersionAndArchiveTime() =
        runTest {
            repository.observeGroup(GROUP_ID).test {
                skipItems(1)

                backend.answerGroupScreen(group(archivedAt = ARCHIVED_AT))

                val group = awaitItem().value
                assertEquals(
                    GroupSettings("1st Street", "America/Phoenix", PaymentOrder.ByDate, 3, 5_000),
                    group?.settings,
                )
                assertEquals(6L, group?.version)
                assertEquals(Instant.fromEpochMilliseconds(ARCHIVED_AT), group?.archivedAt)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun aLaterResultReplacesTheCachedSettings() =
        runTest {
            repository.observeGroup(GROUP_ID).test {
                skipItems(1)
                backend.answerGroupScreen(group())
                skipItems(1)

                backend.answerGroupScreen(group(name = "Renamed", graceDays = 8, version = 7))

                val group = awaitItem().value
                assertEquals("Renamed", group?.settings?.name)
                assertEquals(8, group?.settings?.graceDays)
                assertEquals(7L, group?.version)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun leavingClearsTheCachedGroupAndTheLossIsNotReported() =
        runTest {
            backend.mutationAnswers[LEAVE] = Ok(JsonNull)
            groupAccessWatcher.losses.test {
                repository.observeGroup(GROUP_ID).test {
                    skipItems(1)
                    backend.answerGroupScreen(group())
                    skipItems(1)

                    repository.leaveGroup(GROUP_ID)
                    backend.answerSubscription(GROUP_SCREEN, Err(BackendError.Convex("PermissionDenied", "Gone", null)))

                    cancelAndIgnoreRemainingEvents()
                }

                expectNoEvents()
            }
            assertEquals(null, database.groupDao().observeById(GROUP_ID).first())
        }

    @Test
    fun aFailedLeaveStillReportsLaterLostAccess() =
        runTest {
            backend.mutationAnswers[LEAVE] =
                Err(BackendError.Convex("ValidationFailed", "Transfer ownership before leaving.", null))
            groupAccessWatcher.losses.test {
                repository.observeGroup(GROUP_ID).test {
                    skipItems(1)
                    backend.answerGroupScreen(group())
                    skipItems(1)

                    repository.leaveGroup(GROUP_ID)
                    backend.answerSubscription(
                        GROUP_SCREEN,
                        Err(BackendError.Convex("PermissionDenied", "Removed", null)),
                    )

                    awaitItem()
                    cancelAndIgnoreRemainingEvents()
                }

                awaitItem()
            }
        }

    private fun FakeBackend.answerGroupScreen(group: Group) {
        val result =
            GroupsScreenResult(
                groupSummaries =
                    listOf(
                        GroupSummary(
                            activeTenantCount = 0,
                            archivedOwedCents = 0,
                            archivedTenantCount = 0,
                            groupId = GROUP_ID,
                            lateTenantCount = 0,
                            totalOwedCents = 0,
                        ),
                    ),
                groups = listOf(group),
                memberships = listOf(owner()),
                tenantSummaries = emptyList(),
                tenants = emptyList(),
            )
        answerSubscription(GROUP_SCREEN, Ok(Json.encodeToJsonElement(GroupsScreenResult.serializer(), result)))
    }

    private fun group(
        name: String = "1st Street",
        graceDays: Long = 3,
        version: Long = 6,
        archivedAt: Long? = null,
    ) = Group(
        creationTime = 1.25,
        id = GROUP_ID,
        archivedAt = archivedAt,
        graceDays = graceDays,
        lateFeeFlatCents = 5_000,
        name = name,
        ownerId = "me",
        paymentOrder = GroupPaymentOrder.ByDate,
        timeZone = "America/Phoenix",
        version = version,
    )

    private fun owner() =
        Membership(
            creationTime = 1.25,
            id = "m1",
            accountId = "me",
            debts = MembershipDebts.Full,
            editOthers = true,
            groupId = GROUP_ID,
            members = MembershipMembers.Full,
            notificationsOn = true,
            payments = MembershipPayments.Full,
            rentTerms = MembershipRentTerms.Full,
            role = MembershipRole.Owner,
            settings = MembershipSettings.Full,
            tenants = MembershipTenants.Full,
            version = 1,
        )

    private companion object {
        const val GROUP_SCREEN = "groups:screen"
        const val UPDATE_SETTINGS = "groups:updateSettings"
        const val ARCHIVE = "groups:archive"
        const val UNARCHIVE = "groups:unarchive"
        const val LEAVE = "members:leave"
        const val GROUP_ID = "g1"
        const val TEN_O_CLOCK = 1_794_736_800_000L
        const val ARCHIVED_AT = 1_767_625_200_000L
    }
}
