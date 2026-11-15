package net.neednot.renttracker.core.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MembersStoreTest {
    private val database =
        Room
            .inMemoryDatabaseBuilder<RentTrackerDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()

    private val store = ScreenResultStore(database)

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun aMembersResultListsTheOwnerFirstThenMembersInJoinOrder() =
        runTest {
            store.replaceMembers(
                groupId = "g1",
                memberships =
                    listOf(
                        member("m3", accountId = "lee", creationTime = 3),
                        member("m1", accountId = "sam", creationTime = 1),
                        member("m2", accountId = "dana", creationTime = 2, role = MembershipRole.Owner),
                    ),
                memberProfiles = emptyList(),
                invites = emptyList(),
            )

            assertEquals(listOf("m2", "m1", "m3"), membershipIds("g1"))
        }

    @Test
    fun aMembersResultReplacesTheGroupsMembershipsAndInvitesButNotOtherGroups() =
        runTest {
            store.replaceMembers(
                "g1",
                listOf(member("m1"), member("m2", accountId = "sam")),
                emptyList(),
                listOf(invite("i1")),
            )
            store.replaceMembers("g2", listOf(member("m9", groupId = "g2")), emptyList(), listOf(invite("i9", "g2")))

            store.replaceMembers("g1", listOf(member("m1")), emptyList(), listOf(invite("i2")))

            assertEquals(listOf("m1"), membershipIds("g1"))
            assertEquals(listOf("m9"), membershipIds("g2"))
            assertEquals(
                listOf("i2"),
                database
                    .inviteDao()
                    .observeByGroupId("g1")
                    .first()
                    .map { it.id },
            )
            assertEquals(
                listOf("i9"),
                database
                    .inviteDao()
                    .observeByGroupId("g2")
                    .first()
                    .map { it.id },
            )
        }

    @Test
    fun memberProfilesAreReadThroughTheGroupsMemberships() =
        runTest {
            val sam = MemberProfileEntity(accountId = "sam", displayName = "Sam Rivera", email = "sam@example.com")
            val lee = MemberProfileEntity(accountId = "lee", displayName = "Lee", email = null)
            store.replaceMembers("g1", listOf(member("m1", accountId = "sam")), listOf(sam), emptyList())
            store.replaceMembers(
                "g2",
                listOf(member("m2", groupId = "g2", accountId = "lee")),
                listOf(lee),
                emptyList(),
            )

            assertEquals(listOf(sam), database.memberProfileDao().observeByGroupId("g1").first())
        }

    @Test
    fun aGroupResultNamesTheCallerWithoutDroppingOtherMembers() =
        runTest {
            store.replaceMembers(
                "g1",
                listOf(member("m1", accountId = "me"), member("m2", accountId = "sam")),
                emptyList(),
                emptyList(),
            )

            replaceGroup(caller = member("m1", accountId = "me", role = MembershipRole.Owner))

            assertEquals(
                "m1",
                database
                    .membershipDao()
                    .observeCaller("g1")
                    .first()
                    ?.id,
            )
            assertEquals(
                MembershipRole.Owner,
                database
                    .membershipDao()
                    .observeCaller("g1")
                    .first()
                    ?.role,
            )
            assertEquals(listOf("m1", "m2"), membershipIds("g1"))
        }

    @Test
    fun aCallerWhoRejoinedKeepsOnlyTheirNewMembership() =
        runTest {
            replaceGroup(caller = member("old", accountId = "me"))

            replaceGroup(caller = member("new", accountId = "me"))

            assertEquals(listOf("new"), membershipIds("g1"))
            assertEquals(
                "new",
                database
                    .membershipDao()
                    .observeCaller("g1")
                    .first()
                    ?.id,
            )
        }

    @Test
    fun aHomeResultReplacesTheInvitations() =
        runTest {
            store.replaceHome(emptyList(), emptyList(), listOf(invitation("i1"), invitation("i2")))

            store.replaceHome(emptyList(), emptyList(), listOf(invitation("i2")))

            assertEquals(
                listOf("i2"),
                database
                    .homeInvitationDao()
                    .observeAll()
                    .first()
                    .map { it.inviteId },
            )
        }

    @Test
    fun clearingAGroupRemovesEverythingCachedForItAndNothingElse() =
        runTest {
            cacheGroupWithATenant("g1", "t1")
            cacheGroupWithATenant("g2", "t2")

            store.clearGroup("g1")

            assertEquals(null, database.groupDao().observeById("g1").first())
            assertEquals(null, database.groupSummaryDao().observeByGroupId("g1").first())
            assertEquals(null, database.tenantDao().observeById("t1").first())
            assertEquals(null, database.tenantSummaryDao().observeByTenantId("t1").first())
            assertEquals(emptyList(), database.paymentDao().observeByTenantId("t1").first())
            assertEquals(emptyList(), database.debtDao().observeByTenantId("t1").first())
            assertEquals(emptyList(), database.ledgerRowDao().observeByTenantId("t1").first())
            assertEquals(emptyList(), membershipIds("g1"))
            assertEquals(emptyList(), database.inviteDao().observeByGroupId("g1").first())
            assertEquals(groupEntity("g2"), database.groupDao().observeById("g2").first())
            assertEquals(
                listOf("p-t2"),
                database
                    .paymentDao()
                    .observeByTenantId("t2")
                    .first()
                    .map { it.id },
            )
            assertEquals(listOf("m-g2"), membershipIds("g2"))
        }

    private suspend fun cacheGroupWithATenant(
        groupId: String,
        tenantId: String,
    ) {
        store.replaceGroup(
            groupId = groupId,
            scope =
                GroupScopeEntities(
                    groups = listOf(groupEntity(groupId)),
                    tenants = listOf(tenantEntity(tenantId, groupId = groupId)),
                    tenantSummaries = listOf(tenantSummaryEntity(tenantId)),
                    groupSummaries = listOf(groupSummaryEntity(groupId)),
                    callerMemberships = listOf(member("m-$groupId", groupId = groupId)),
                ),
        )
        store.replaceTenant(
            tenantId = tenantId,
            groups = listOf(groupEntity(groupId)),
            tenants = listOf(tenantEntity(tenantId, groupId = groupId)),
            tenantSummaries = listOf(tenantSummaryEntity(tenantId)),
            scope =
                TenantScopeEntities(
                    rentTerms = listOf(rentTermEntity("r-$tenantId", tenantId = tenantId)),
                    payments = listOf(paymentEntity("p-$tenantId", tenantId)),
                    debts = listOf(debtEntity("d-$tenantId", tenantId)),
                    ledgerRows = listOf(ledgerRowEntity(tenantId, "rent:2026-03", 1)),
                    accountNames = emptyList(),
                    callerMemberships = listOf(member("m-$groupId", groupId = groupId)),
                    allocations = emptyList(),
                    calendarMonths = emptyList(),
                ),
        )
        store.replaceMembers(
            groupId,
            listOf(member("m-$groupId", groupId = groupId)),
            emptyList(),
            listOf(invite("i-$groupId", groupId)),
        )
    }

    private suspend fun replaceGroup(caller: MembershipEntity) {
        store.replaceGroup(
            groupId = "g1",
            scope =
                GroupScopeEntities(
                    groups = listOf(groupEntity("g1")),
                    tenants = emptyList(),
                    tenantSummaries = emptyList(),
                    groupSummaries = listOf(groupSummaryEntity("g1")),
                    callerMemberships = listOf(caller),
                ),
        )
    }

    private suspend fun membershipIds(groupId: String): List<String> =
        database
            .membershipDao()
            .observeByGroupId(groupId)
            .first()
            .map { it.id }

    private fun member(
        id: String,
        groupId: String = "g1",
        accountId: String = "me",
        creationTime: Long = 0,
        role: MembershipRole = MembershipRole.Member,
    ) = membershipEntity(id, groupId, role, accountId = accountId, creationTime = creationTime)

    private fun invite(
        id: String,
        groupId: String = "g1",
    ) = InviteEntity(
        id = id,
        groupId = groupId,
        creationTime = 0,
        email = "$id@example.com",
        permissions = fullPermissions(),
        invitedBy = "me",
        expiresAt = 0,
    )

    private fun invitation(inviteId: String) =
        HomeInvitationEntity(
            inviteId = inviteId,
            groupId = "g1",
            groupName = "1st Street",
            invitedByName = "Dana",
            permissions = fullPermissions(),
            expiresAt = 0,
        )
}
