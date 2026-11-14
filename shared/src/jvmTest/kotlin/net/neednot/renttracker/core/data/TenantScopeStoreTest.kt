package net.neednot.renttracker.core.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TenantScopeStoreTest {
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
    fun aTenantResultWritesPaymentsDebtsLedgerRowsAccountsAndMembership() =
        runTest {
            val payment = paymentEntity("p1", tenantId = "t1")
            val debt = debtEntity("d1", tenantId = "t1", kind = DebtKind.LateFee)
            val row = ledgerRowEntity("t1", key = "payment:p1", position = 1, kind = LedgerRowKind.Payment)
            val account = accountNameEntity("a1")
            val membership = membershipEntity("m1", groupId = "g1")

            replaceTenant("t1", listOf(payment), listOf(debt), listOf(row), listOf(account), listOf(membership))

            assertEquals(listOf(payment), database.paymentDao().observeByTenantId("t1").first())
            assertEquals(payment, database.paymentDao().observeById("p1").first())
            assertEquals(listOf(debt), database.debtDao().observeByTenantId("t1").first())
            assertEquals(debt, database.debtDao().observeById("d1").first())
            assertEquals(listOf(row), database.ledgerRowDao().observeByTenantId("t1").first())
            assertEquals(listOf(account), database.accountNameDao().observeAll().first())
            assertEquals(listOf(account), database.accountNameDao().observeByIds(listOf("a1", "other")).first())
            assertEquals(membership, database.membershipDao().observeByGroupId("g1").first())
        }

    @Test
    fun aNewerTenantResultRemovesRowsMissingFromIt() =
        runTest {
            replaceTenant(
                "t1",
                payments = listOf(paymentEntity("p1", "t1"), paymentEntity("p2", "t1")),
                debts = listOf(debtEntity("d1", "t1"), debtEntity("d2", "t1")),
                ledgerRows = listOf(ledgerRowEntity("t1", "rent:2026-03", 1), ledgerRowEntity("t1", "debt:d1", 2)),
                memberships = listOf(membershipEntity("m1", "g1")),
            )

            replaceTenant(
                "t1",
                payments = listOf(paymentEntity("p2", "t1")),
                debts = listOf(debtEntity("d2", "t1")),
                ledgerRows = listOf(ledgerRowEntity("t1", "rent:2026-03", 1)),
                memberships = listOf(membershipEntity("m2", "g1", MembershipRole.Member)),
            )

            assertEquals(
                listOf("p2"),
                database
                    .paymentDao()
                    .observeByTenantId("t1")
                    .first()
                    .map { it.id },
            )
            assertEquals(
                listOf("d2"),
                database
                    .debtDao()
                    .observeByTenantId("t1")
                    .first()
                    .map { it.id },
            )
            assertEquals(
                listOf("rent:2026-03"),
                database
                    .ledgerRowDao()
                    .observeByTenantId("t1")
                    .first()
                    .map { it.key },
            )
            assertEquals(
                "m2",
                database
                    .membershipDao()
                    .observeByGroupId("g1")
                    .first()
                    ?.id,
            )
        }

    @Test
    fun aTenantResultLeavesOtherTenantsAndDismissedSuggestionsUntouched() =
        runTest {
            replaceTenant(
                "t1",
                payments = listOf(paymentEntity("p1", "t1")),
                debts = listOf(debtEntity("d1", "t1")),
                ledgerRows = listOf(ledgerRowEntity("t1", "rent:2026-03", 1)),
            )
            replaceTenant(
                "t2",
                payments = listOf(paymentEntity("p2", "t2")),
                debts = listOf(debtEntity("d2", "t2")),
                ledgerRows = listOf(ledgerRowEntity("t2", "rent:2026-03", 1)),
            )
            database.dismissedLateFeeSuggestionDao().insert(DismissedLateFeeSuggestionEntity("t1", "2026-03"))

            replaceTenant("t1")

            assertEquals(emptyList(), database.paymentDao().observeByTenantId("t1").first())
            assertEquals(emptyList(), database.debtDao().observeByTenantId("t1").first())
            assertEquals(emptyList(), database.ledgerRowDao().observeByTenantId("t1").first())
            assertEquals(
                listOf("p2"),
                database
                    .paymentDao()
                    .observeByTenantId("t2")
                    .first()
                    .map { it.id },
            )
            assertEquals(
                listOf("d2"),
                database
                    .debtDao()
                    .observeByTenantId("t2")
                    .first()
                    .map { it.id },
            )
            assertEquals(
                1,
                database
                    .ledgerRowDao()
                    .observeByTenantId("t2")
                    .first()
                    .size,
            )
            assertEquals(
                listOf("2026-03"),
                database.dismissedLateFeeSuggestionDao().observeDismissedMonths("t1").first(),
            )
        }

    @Test
    fun ledgerRowsComeBackNewestFirst() =
        runTest {
            replaceTenant(
                "t1",
                ledgerRows =
                    listOf(
                        ledgerRowEntity("t1", "rent:2026-01", position = 1),
                        ledgerRowEntity("t1", "payment:p1", position = 3),
                        ledgerRowEntity("t1", "rent:2026-02", position = 2),
                    ),
            )

            assertEquals(
                listOf("payment:p1", "rent:2026-02", "rent:2026-01"),
                database
                    .ledgerRowDao()
                    .observeByTenantId("t1")
                    .first()
                    .map { it.key },
            )
        }

    @Test
    fun theSameLedgerKeyCanExistForTwoTenants() =
        runTest {
            replaceTenant("t1", ledgerRows = listOf(ledgerRowEntity("t1", "rent:2026-03", 1)))
            replaceTenant("t2", ledgerRows = listOf(ledgerRowEntity("t2", "rent:2026-03", 1)))

            assertEquals(
                1,
                database
                    .ledgerRowDao()
                    .observeByTenantId("t1")
                    .first()
                    .size,
            )
            assertEquals(
                1,
                database
                    .ledgerRowDao()
                    .observeByTenantId("t2")
                    .first()
                    .size,
            )
        }

    private suspend fun replaceTenant(
        tenantId: String,
        payments: List<PaymentEntity> = emptyList(),
        debts: List<DebtEntity> = emptyList(),
        ledgerRows: List<LedgerRowEntity> = emptyList(),
        accountNames: List<AccountNameEntity> = emptyList(),
        memberships: List<MembershipEntity> = emptyList(),
    ) {
        store.replaceTenant(
            tenantId = tenantId,
            groups = listOf(groupEntity("g1")),
            tenants = listOf(tenantEntity(tenantId, groupId = "g1")),
            tenantSummaries = listOf(tenantSummaryEntity(tenantId)),
            scope = TenantScopeEntities(emptyList(), payments, debts, ledgerRows, accountNames, memberships),
        )
    }
}
