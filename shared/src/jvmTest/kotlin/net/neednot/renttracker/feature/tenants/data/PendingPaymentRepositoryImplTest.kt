package net.neednot.renttracker.feature.tenants.data

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.data.DebtKind
import net.neednot.renttracker.core.data.DuplicateMatchKind
import net.neednot.renttracker.core.data.PaymentMethod
import net.neednot.renttracker.core.data.PendingConfirmationReason
import net.neednot.renttracker.core.data.PendingDuplicateMatchEntity
import net.neednot.renttracker.core.data.PendingPaymentEntity
import net.neednot.renttracker.core.data.PendingPaymentQueue
import net.neednot.renttracker.core.data.PendingPaymentReader
import net.neednot.renttracker.core.data.PendingPaymentStatus
import net.neednot.renttracker.core.data.PendingRejectionReason
import net.neednot.renttracker.core.data.RecordingSyncScheduler
import net.neednot.renttracker.core.data.SignedInAccountEntity
import net.neednot.renttracker.core.data.accountNameEntity
import net.neednot.renttracker.core.data.debtEntity
import net.neednot.renttracker.core.data.inMemoryDatabase
import net.neednot.renttracker.core.data.membershipEntity
import net.neednot.renttracker.core.data.paymentEntity
import net.neednot.renttracker.core.data.pendingPaymentEntity
import net.neednot.renttracker.core.data.tenantEntity
import net.neednot.renttracker.core.data.tenantSummaryEntity
import net.neednot.renttracker.feature.tenants.domain.ChargeKind
import net.neednot.renttracker.feature.tenants.domain.PendingBalanceChange
import net.neednot.renttracker.feature.tenants.domain.PendingDuplicateMatch
import net.neednot.renttracker.feature.tenants.domain.PendingNewEntry
import net.neednot.renttracker.feature.tenants.domain.PendingPayment
import net.neednot.renttracker.feature.tenants.domain.PendingPaymentReview
import net.neednot.renttracker.feature.tenants.domain.PendingRejection
import net.neednot.renttracker.feature.tenants.domain.PendingState
import net.neednot.renttracker.feature.tenants.domain.Person
import net.neednot.renttracker.feature.tenants.domain.TenantError
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class PendingPaymentRepositoryImplTest {
    private val database = inMemoryDatabase()
    private val repository =
        PendingPaymentRepositoryImpl(
            database = database,
            reader = PendingPaymentReader(database),
            queue = PendingPaymentQueue(database, RecordingSyncScheduler()),
        )

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun aTenantsPendingPaymentsMapToTheirStatesInLoggedOrder() =
        runTest {
            insert(
                pending("c1", sequence = 1),
                pending("c2", sequence = 2, status = PendingPaymentStatus.NeedsConfirmation)
                    .copy(confirmationReason = PendingConfirmationReason.PossibleDuplicate),
                pending("c3", sequence = 3, status = PendingPaymentStatus.NeedsConfirmation)
                    .copy(confirmationReason = PendingConfirmationReason.TenantChanged),
                pending("c4", sequence = 4, status = PendingPaymentStatus.Rejected)
                    .copy(rejectionReason = PendingRejectionReason.TenantMoved),
                pending("c5", sequence = 5, status = PendingPaymentStatus.Rejected)
                    .copy(rejectionReason = PendingRejectionReason.Other, rejectionMessage = "Group was deleted"),
                pending("other", sequence = 6, tenantId = "t2"),
            )

            val payments = repository.observePendingPayments("t1").first()

            assertEquals(
                listOf(
                    PendingState.Waiting,
                    PendingState.PossibleDuplicate,
                    PendingState.TenantChanged,
                    PendingState.Rejected(PendingRejection.TenantMoved),
                    PendingState.Rejected(PendingRejection.Other("Group was deleted")),
                ),
                payments.map { it.state },
            )
            assertEquals(
                PendingPayment(
                    clientId = "c1",
                    tenantName = "Apt 3B",
                    amountCents = 55_000,
                    dateReceived = LocalDate(2026, 3, 10),
                    method = PaymentMethod.Zelle,
                    state = PendingState.Waiting,
                ),
                payments.first(),
            )
        }

    @Test
    fun aPossibleDuplicateReviewCarriesTheStoredMatches() =
        runTest {
            insert(
                pending("c1", status = PendingPaymentStatus.NeedsConfirmation)
                    .copy(confirmationReason = PendingConfirmationReason.PossibleDuplicate),
            )
            database.pendingPaymentDao().insertDuplicateMatches(
                listOf(
                    PendingDuplicateMatchEntity(
                        clientId = "c1",
                        matchId = "p9",
                        kind = DuplicateMatchKind.Payment,
                        date = "2026-03-09",
                        amountCents = 55_000,
                        method = PaymentMethod.Zelle,
                        byName = "Sam Rivera",
                        byAccountId = "sam",
                    ),
                ),
            )

            val review = assertIs<PendingPaymentReview.PossibleDuplicate>(repository.observeReview("c1").first())

            assertEquals(
                listOf(
                    PendingDuplicateMatch(
                        kind = DuplicateMatchKind.Payment,
                        date = LocalDate(2026, 3, 9),
                        amountCents = 55_000,
                        method = PaymentMethod.Zelle,
                        byName = "Sam Rivera",
                    ),
                ),
                review.matches,
            )
        }

    @Test
    fun aChangedTenantReviewListsEntriesMissingFromTheFingerprintAndTheBalanceChange() =
        runTest {
            cacheTenant(balanceOwedCents = 0)
            database.paymentDao().upsert(
                listOf(
                    paymentEntity("p1", "t1"),
                    paymentEntity("p2", "t1", amountCents = 55_000).copy(dateReceived = "2026-03-10", loggedBy = "sam"),
                    paymentEntity("p3", "t1").copy(deletedAt = 1),
                ),
            )
            database.debtDao().upsert(
                listOf(
                    debtEntity("d1", "t1"),
                    debtEntity("d2", "t1", kind = DebtKind.LateFee).copy(forMonth = "2026-03", createdBy = "me"),
                ),
            )
            insert(
                pending("c1", status = PendingPaymentStatus.NeedsConfirmation).copy(
                    confirmationReason = PendingConfirmationReason.TenantChanged,
                    tenantFingerprint = "debt:d1:1,payment:p1:1,tenant:1",
                    balanceCentsWhenLogged = 55_000,
                ),
            )

            val review = assertIs<PendingPaymentReview.TenantChanged>(repository.observeReview("c1").first())

            assertEquals(PendingBalanceChange(wasCents = 55_000, nowCents = 0), review.balanceChange)
            assertEquals(
                listOf(
                    PendingNewEntry.Payment(LocalDate(2026, 3, 10), 55_000, Person.Named("Sam Rivera")),
                    PendingNewEntry.Charge(
                        LocalDate(2026, 3, 10),
                        5_000,
                        Person.You,
                        ChargeKind.LateFee(YearMonth(2026, 3)),
                    ),
                ),
                review.newEntries,
            )
        }

    @Test
    fun aChangedTenantWithoutALoggedBalanceShowsNoBalanceChange() =
        runTest {
            cacheTenant(balanceOwedCents = 0)
            insert(
                pending("c1", status = PendingPaymentStatus.NeedsConfirmation)
                    .copy(confirmationReason = PendingConfirmationReason.TenantChanged, balanceCentsWhenLogged = null),
            )

            val review = assertIs<PendingPaymentReview.TenantChanged>(repository.observeReview("c1").first())

            assertNull(review.balanceChange)
        }

    @Test
    fun aRejectedReviewCarriesItsReason() =
        runTest {
            insert(
                pending("c1", status = PendingPaymentStatus.Rejected)
                    .copy(rejectionReason = PendingRejectionReason.NoPermission),
            )

            val review = assertIs<PendingPaymentReview.Rejected>(repository.observeReview("c1").first())

            assertEquals(PendingRejection.NoPermission, review.reason)
        }

    @Test
    fun aPaymentThatIsGoneHasNoReview() =
        runTest {
            assertNull(repository.observeReview("missing").first())
        }

    @Test
    fun confirmingAPossibleDuplicateSendsItAgainAsConfirmed() =
        runTest {
            cacheTenant(balanceOwedCents = 55_000)
            insert(
                pending("c1", status = PendingPaymentStatus.NeedsConfirmation)
                    .copy(confirmationReason = PendingConfirmationReason.PossibleDuplicate),
            )

            val result = repository.confirm("c1")
            val stored = database.pendingPaymentDao().byClientId("c1")

            assertEquals(Ok(Unit), result)
            assertEquals(PendingPaymentStatus.Pending, stored?.status)
            assertEquals(true, stored?.duplicateConfirmed)
        }

    @Test
    fun confirmingAMissingPaymentIsNotFound() =
        runTest {
            assertEquals(Err(TenantError.NotFound), repository.confirm("missing"))
        }

    @Test
    fun discardingRemovesThePaymentFromThePhone() =
        runTest {
            insert(pending("c1"))

            repository.discard("c1")

            assertEquals(emptyList(), repository.observePendingPayments("t1").first())
        }

    private suspend fun cacheTenant(balanceOwedCents: Long) {
        database.tenantDao().upsert(listOf(tenantEntity("t1", "g1", name = "Apt 3B")))
        database.tenantSummaryDao().upsert(listOf(tenantSummaryEntity("t1", rentOwedCents = balanceOwedCents)))
        database.accountNameDao().upsert(listOf(accountNameEntity("sam", "Sam Rivera")))
        database.membershipDao().upsert(listOf(membershipEntity("m1", "g1", accountId = "me")))
        database.signedInAccountDao().upsert(SignedInAccountEntity("me"))
    }

    private suspend fun insert(vararg payments: PendingPaymentEntity) {
        payments.forEach { database.pendingPaymentDao().insert(it) }
    }

    private fun pending(
        clientId: String,
        sequence: Long = 1,
        tenantId: String = "t1",
        status: PendingPaymentStatus = PendingPaymentStatus.Pending,
    ) = pendingPaymentEntity(clientId, tenantId, sequence).copy(
        tenantName = "Apt 3B",
        amountCents = 55_000,
        dateReceived = "2026-03-10",
        method = PaymentMethod.Zelle,
        status = status,
    )
}
