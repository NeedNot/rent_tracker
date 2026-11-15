package net.neednot.renttracker.feature.members.data

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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.neednot.renttracker.core.access.PermissionPreset
import net.neednot.renttracker.core.backend.BackendError
import net.neednot.renttracker.core.backend.FakeBackend
import net.neednot.renttracker.core.data.AccessLevel
import net.neednot.renttracker.core.data.AccessLossReason
import net.neednot.renttracker.core.data.FakeAsOfClock
import net.neednot.renttracker.core.data.GroupAccessWatcher
import net.neednot.renttracker.core.data.LostGroupAccess
import net.neednot.renttracker.core.data.MembershipRole
import net.neednot.renttracker.core.data.RentTrackerDatabase
import net.neednot.renttracker.core.data.ScreenResultStore
import net.neednot.renttracker.core.data.SettingsAccessLevel
import net.neednot.renttracker.core.data.SignedInAccountEntity
import net.neednot.renttracker.core.data.groupEntity
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.telemetry.NoOpTelemetry
import net.neednot.renttracker.feature.members.domain.Member
import net.neednot.renttracker.feature.members.domain.MembersError
import net.neednot.renttracker.feature.members.domain.PendingInvite
import net.neednot.renttracker.model.members.Invite
import net.neednot.renttracker.model.members.InvitePermissions
import net.neednot.renttracker.model.members.InvitePermissionsDebts
import net.neednot.renttracker.model.members.InvitePermissionsMembers
import net.neednot.renttracker.model.members.InvitePermissionsPayments
import net.neednot.renttracker.model.members.InvitePermissionsRentTerms
import net.neednot.renttracker.model.members.InvitePermissionsSettings
import net.neednot.renttracker.model.members.InvitePermissionsTenants
import net.neednot.renttracker.model.members.MemberProfile
import net.neednot.renttracker.model.members.MembersScreenResult
import net.neednot.renttracker.model.shared.Membership
import net.neednot.renttracker.model.shared.MembershipDebts
import net.neednot.renttracker.model.shared.MembershipMembers
import net.neednot.renttracker.model.shared.MembershipPayments
import net.neednot.renttracker.model.shared.MembershipRentTerms
import net.neednot.renttracker.model.shared.MembershipSettings
import net.neednot.renttracker.model.shared.MembershipTenants
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import net.neednot.renttracker.model.shared.MembershipRole as ConvexMembershipRole

class MembersRepositoryImplTest {
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
        MembersRepositoryImpl(
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

    private suspend fun cacheGroupAndSignIn() {
        database.groupDao().upsert(listOf(groupEntity(GROUP_ID, name = "1st Street")))
        database.signedInAccountDao().upsert(SignedInAccountEntity("me"))
    }

    @Test
    fun theScreenIsCachedOwnerFirstThenInJoinOrderWithProfilesAndInvites() =
        runTest {
            cacheGroupAndSignIn()
            repository.observeMembers(GROUP_ID).test {
                skipItems(1)

                backend.answerMembersScreen(
                    memberships =
                        listOf(
                            membership("m3", "jen", creationTime = 3.0),
                            membership("m1", "me", role = ConvexMembershipRole.Owner, creationTime = 9.0),
                            membership("m2", "sam", creationTime = 2.0),
                        ),
                    invites = listOf(invite("i1", "alex.m@example.com")),
                )

                val overview = awaitItem().value
                assertEquals(listOf("m1", "m2", "m3"), overview?.members?.map { it.membershipId })
                assertEquals("1st Street", overview?.groupName)
                assertEquals(true, overview?.access?.isOwner)
                assertEquals(
                    Member(
                        membershipId = "m2",
                        accountId = "sam",
                        displayName = "Name sam",
                        email = "sam@example.com",
                        role = MembershipRole.Member,
                        permissions = PermissionPreset.Bookkeeper.permissions,
                        version = 2,
                    ),
                    overview?.member("m2"),
                )
                assertEquals(
                    listOf(
                        PendingInvite(
                            inviteId = "i1",
                            email = "alex.m@example.com",
                            permissions = PermissionPreset.Viewer.permissions,
                            expiresAt = Instant.fromEpochMilliseconds(EXPIRES_AT),
                        ),
                    ),
                    overview?.invites,
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun aLaterResultDropsRemovedMembersAndRevokedInvites() =
        runTest {
            cacheGroupAndSignIn()
            repository.observeMembers(GROUP_ID).test {
                skipItems(1)
                backend.answerMembersScreen(
                    memberships = listOf(membership("m1", "me"), membership("m2", "sam")),
                    invites = listOf(invite("i1", "alex.m@example.com")),
                )
                skipItems(1)

                backend.answerMembersScreen(memberships = listOf(membership("m1", "me")), invites = emptyList())

                val overview = awaitItem().value
                assertEquals(listOf("m1"), overview?.members?.map { it.membershipId })
                assertEquals(emptyList(), overview?.invites)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun subscribesToTheMembersScreenWithTheCurrentHour() =
        runTest {
            repository.observeMembers(GROUP_ID).test {
                skipItems(1)
                cancelAndIgnoreRemainingEvents()
            }

            val expectedArgs =
                buildJsonObject {
                    put("asOf", TEN_O_CLOCK)
                    put("groupId", GROUP_ID)
                }
            assertEquals(listOf(SCREEN to expectedArgs), backend.subscriptions)
        }

    @Test
    fun aRemovedCallerLosesTheCachedGroupAndTheAppIsTold() =
        runTest {
            cacheGroupAndSignIn()
            groupAccessWatcher.losses.test {
                repository.observeMembers(GROUP_ID).test {
                    skipItems(1)
                    backend.answerSubscription(SCREEN, Err(convexError("PermissionDenied")))

                    var item = awaitItem()
                    while (item.status !is SyncStatus.Failed) item = awaitItem()
                    assertEquals(SyncStatus.Failed(MembersError.PermissionDenied("PermissionDenied")), item.status)
                    cancelAndIgnoreRemainingEvents()
                }

                assertEquals(LostGroupAccess(GROUP_ID, "1st Street", AccessLossReason.Removed), awaitItem())
            }
            assertEquals(null, database.groupDao().observeById(GROUP_ID).first())
        }

    @Test
    fun sendingAnInviteConvertsEveryLevelForTheServer() =
        runTest {
            backend.mutationAnswers[SEND] = Ok(Json.parseToJsonElement("\"i9\""))
            val permissions =
                PermissionPreset.Manager.permissions.copy(
                    members = AccessLevel.AddEdit,
                    debts = AccessLevel.Full,
                    settings = SettingsAccessLevel.Full,
                )

            val result = repository.sendInvite(GROUP_ID, "alex.m@example.com", permissions)

            val expectedArgs =
                buildJsonObject {
                    put("email", "alex.m@example.com")
                    put("groupId", GROUP_ID)
                    put(
                        "permissions",
                        permissionsJson(
                            "tenants" to "addEdit",
                            "rentTerms" to "addEdit",
                            "payments" to "addEdit",
                            "debts" to "full",
                            "members" to "addEdit",
                            "settings" to "full",
                            editOthers = true,
                        ),
                    )
                }
            assertEquals(Ok(Unit), result)
            assertEquals(listOf(SEND to expectedArgs), backend.mutations)
        }

    @Test
    fun updatingPermissionsSendsTheExpectedVersion() =
        runTest {
            backend.mutationAnswers[UPDATE] = Ok(JsonNull)

            repository.updatePermissions("m2", expectedVersion = 4, permissions = PermissionPreset.Viewer.permissions)

            val expectedArgs =
                buildJsonObject {
                    put("expectedVersion", 4)
                    put("membershipId", "m2")
                    put(
                        "permissions",
                        permissionsJson(
                            "tenants" to "read",
                            "rentTerms" to "read",
                            "payments" to "read",
                            "debts" to "read",
                            "members" to "read",
                            "settings" to "read",
                            editOthers = false,
                        ),
                    )
                }
            assertEquals(listOf(UPDATE to expectedArgs), backend.mutations)
        }

    @Test
    fun revokeRemoveAndTransferSendTheirIds() =
        runTest {
            listOf(REVOKE, REMOVE, TRANSFER).forEach { backend.mutationAnswers[it] = Ok(JsonNull) }

            repository.revokeInvite("i1")
            repository.removeMember("m2")
            repository.transferOwnership(GROUP_ID, "m3")

            assertEquals(
                listOf(
                    REVOKE to buildJsonObject { put("inviteId", "i1") },
                    REMOVE to buildJsonObject { put("membershipId", "m2") },
                    TRANSFER to
                        buildJsonObject {
                            put("groupId", GROUP_ID)
                            put("toMembershipId", "m3")
                        },
                ),
                backend.mutations,
            )
        }

    @Test
    fun serverErrorCodesBecomeMembersErrors() =
        runTest {
            val cases =
                listOf(
                    "PermissionDenied" to MembersError.PermissionDenied(MESSAGE),
                    "NotFound" to MembersError.NotFound(MESSAGE),
                    "EditConflict" to MembersError.EditConflict(MESSAGE, editedByName = null),
                    "ValidationFailed" to MembersError.ValidationFailed(MESSAGE),
                    "Weird" to MembersError.Unexpected("Weird: $MESSAGE"),
                )
            cases.forEach { (code, expected) ->
                backend.mutationAnswers[REMOVE] = Err(BackendError.Convex(code, MESSAGE, null))

                assertEquals(Err(expected), repository.removeMember("m2"), "code = $code")
            }
        }

    @Test
    fun anInviteToAnExistingMemberNamesTheEmailField() =
        runTest {
            val details = buildJsonObject { put("field", "email") }
            backend.mutationAnswers[SEND] =
                Err(BackendError.Convex("ValidationFailed", "Already a member of this group.", details))

            assertEquals(
                Err(MembersError.ValidationFailed("Already a member of this group.", field = "email")),
                repository.sendInvite(GROUP_ID, "maria@example.com", PermissionPreset.Viewer.permissions),
            )
        }

    @Test
    fun anEditConflictCarriesTheNameOfWhoChangedThePermissions() =
        runTest {
            val details =
                buildJsonObject {
                    put("currentVersion", 5)
                    put("lastEditedByName", "Sam Rivera")
                }
            backend.mutationAnswers[UPDATE] = Err(BackendError.Convex("EditConflict", MESSAGE, details))

            assertEquals(
                Err(MembersError.EditConflict(MESSAGE, editedByName = "Sam Rivera")),
                repository.updatePermissions("m2", 4, PermissionPreset.Viewer.permissions),
            )
        }

    @Test
    fun aChangeWithoutAConnectionReportsOffline() =
        runTest {
            backend.isReachable = false

            assertEquals(Err(MembersError.Offline), repository.revokeInvite("i1"))
        }

    private fun FakeBackend.answerMembersScreen(
        memberships: List<Membership>,
        invites: List<Invite>,
    ) {
        val result =
            MembersScreenResult(
                invites = invites,
                memberProfiles =
                    memberships.map {
                        MemberProfile(
                            it.accountId,
                            "Name ${it.accountId}",
                            "${it.accountId}@example.com",
                        )
                    },
                memberships = memberships,
            )
        answerSubscription(SCREEN, Ok(Json.encodeToJsonElement(MembersScreenResult.serializer(), result)))
    }

    private fun membership(
        id: String,
        accountId: String,
        role: ConvexMembershipRole = ConvexMembershipRole.Member,
        creationTime: Double = 1.0,
    ) = Membership(
        creationTime = creationTime,
        id = id,
        accountId = accountId,
        debts = MembershipDebts.AddEdit,
        editOthers = false,
        groupId = GROUP_ID,
        members = MembershipMembers.Read,
        notificationsOn = true,
        payments = MembershipPayments.AddEdit,
        rentTerms = MembershipRentTerms.Read,
        role = role,
        settings = MembershipSettings.Read,
        tenants = MembershipTenants.Read,
        version = 2,
    )

    private fun invite(
        id: String,
        email: String,
    ) = Invite(
        creationTime = 1.0,
        id = id,
        email = email,
        expiresAt = EXPIRES_AT,
        groupId = GROUP_ID,
        invitedBy = "me",
        permissions =
            InvitePermissions(
                debts = InvitePermissionsDebts.Read,
                editOthers = false,
                members = InvitePermissionsMembers.Read,
                payments = InvitePermissionsPayments.Read,
                rentTerms = InvitePermissionsRentTerms.Read,
                settings = InvitePermissionsSettings.Read,
                tenants = InvitePermissionsTenants.Read,
            ),
    )

    private fun permissionsJson(
        vararg levels: Pair<String, String>,
        editOthers: Boolean,
    ): JsonObject =
        buildJsonObject {
            levels.forEach { (category, level) -> put(category, level) }
            put("editOthers", editOthers)
        }

    private fun convexError(code: String) = BackendError.Convex(code = code, message = code, details = null)

    private companion object {
        const val GROUP_ID = "g1"
        const val SCREEN = "members:screen"
        const val SEND = "invites:send"
        const val REVOKE = "invites:revoke"
        const val UPDATE = "members:updatePermissions"
        const val REMOVE = "members:remove"
        const val TRANSFER = "members:transferOwnership"
        const val MESSAGE = "Nope."
        const val TEN_O_CLOCK = 1_794_736_800_000L
        const val EXPIRES_AT = 1_795_000_000_000L
    }
}
