package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.ui.MainDispatcherTest
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.tenants.domain.ChargeKind
import net.neednot.renttracker.feature.tenants.domain.EntryEdit
import net.neednot.renttracker.feature.tenants.domain.EntryStatus
import net.neednot.renttracker.feature.tenants.domain.FakeTenantRepository
import net.neednot.renttracker.feature.tenants.domain.LedgerBalance
import net.neednot.renttracker.feature.tenants.domain.LedgerEntry
import net.neednot.renttracker.feature.tenants.domain.Person
import net.neednot.renttracker.feature.tenants.domain.TenantError
import net.neednot.renttracker.feature.tenants.domain.TenantOverview
import net.neednot.renttracker.feature.tenants.domain.callerPermissions
import net.neednot.renttracker.feature.tenants.domain.chargeEntry
import net.neednot.renttracker.feature.tenants.domain.paymentEntry
import net.neednot.renttracker.feature.tenants.domain.rentEntry
import net.neednot.renttracker.feature.tenants.domain.synced
import net.neednot.renttracker.feature.tenants.domain.tenantBalance
import net.neednot.renttracker.feature.tenants.domain.tenantOverview
import net.neednot.renttracker.feature.tenants.presentation.uistate.EntryChip
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerRowUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.balance_credit
import net.neednot.renttracker.resources.balance_owes
import net.neednot.renttracker.resources.balance_paid_up
import net.neednot.renttracker.resources.ledger_amount_paid
import net.neednot.renttracker.resources.ledger_cancelled_named
import net.neednot.renttracker.resources.ledger_custom_added_you
import net.neednot.renttracker.resources.ledger_date_by_you
import net.neednot.renttracker.resources.ledger_edited_named
import net.neednot.renttracker.resources.ledger_edited_you
import net.neednot.renttracker.resources.ledger_empty_first_rent
import net.neednot.renttracker.resources.ledger_empty_no_date
import net.neednot.renttracker.resources.ledger_late_fee_added_named
import net.neednot.renttracker.resources.ledger_late_fee_title
import net.neednot.renttracker.resources.ledger_payment_logged_named
import net.neednot.renttracker.resources.ledger_payment_logged_you
import net.neednot.renttracker.resources.ledger_payment_received_you
import net.neednot.renttracker.resources.ledger_payment_title
import net.neednot.renttracker.resources.ledger_payment_title_zelle
import net.neednot.renttracker.resources.ledger_rent_title
import net.neednot.renttracker.resources.tenant_error_offline
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class LedgerViewModelTest : MainDispatcherTest() {
    private val repository = FakeTenantRepository()
    private val viewModel by lazy { LedgerViewModel("t1", repository, TimeZone.UTC) }

    @Test
    fun showsLoadingUntilTheTenantArrives() =
        runTest {
            viewModel.uiState.test { assertEquals(LedgerUiState.Loading, awaitItem()) }
        }

    @Test
    fun aFailedLoadWithNothingCachedShowsTheError() =
        runTest {
            repository.tenant.value = Synced(null, SyncStatus.Failed(TenantError.Offline))
            runCurrent()

            val state = viewModel.uiState.filterIsInstance<LedgerUiState.Error>().first()

            assertEquals(UiText.Resource(Res.string.tenant_error_offline), state.message)
        }

    @Test
    fun theTitleNamesTheTenantAndItsGroup() =
        runTest {
            val content = contentOf(tenantOverview())

            assertEquals("Apt 3B", content.tenantName)
            assertEquals("1st Street", content.groupName)
        }

    @Test
    fun entriesAreGroupedUnderMonthHeadersNewestFirst() =
        runTest {
            val ledger =
                listOf(
                    paymentEntry(key = "payment:p2", date = LocalDate(2026, 3, 2)),
                    rentEntry(month = "2026-03", date = LocalDate(2026, 3, 1)),
                    rentEntry(month = "2026-02", date = LocalDate(2026, 2, 15)),
                )

            val content = contentOf(tenantOverview(ledger = ledger))

            assertEquals(listOf("March 2026", "February 2026"), content.sections.map { it.header })
            assertEquals(
                listOf(listOf("payment:p2", "rent:2026-03"), listOf("rent:2026-02")),
                content.sections.map { section -> section.rows.map { it.key } },
            )
            assertNull(content.emptyMessage)
        }

    @Test
    fun aRentRowIsNotTappableAndShowsTheMonthAndBalance() =
        runTest {
            val row = rowOf(rentEntry(month = "2026-03", balance = LedgerBalance.Owed(205_000)))

            assertEquals(
                LedgerRowUiState(
                    key = "rent:2026-03",
                    date = "Mar 1",
                    title = UiText.Resource(Res.string.ledger_rent_title, listOf("March")),
                    details = emptyList(),
                    amount = UiText.Plain("$1,500.00"),
                    balance = UiText.Resource(Res.string.balance_owes, listOf("$2,050.00")),
                    chips = emptyList(),
                    isStruckThrough = false,
                    mayNotApply = false,
                    canRestore = false,
                    isTappable = false,
                ),
                row,
            )
        }

    @Test
    fun aPaymentShowsItsMethodWhoLoggedItAndWhatItPaid() =
        runTest {
            val row = rowOf(paymentEntry(balance = LedgerBalance.Owed(67_000)))

            assertEquals(UiText.Resource(Res.string.ledger_payment_title_zelle), row.title)
            assertEquals(
                listOf(UiText.Resource(Res.string.ledger_payment_logged_you, listOf("Mar 2"))),
                row.details,
            )
            assertEquals(UiText.Resource(Res.string.ledger_amount_paid, listOf("$1,500.00")), row.amount)
            assertEquals(UiText.Resource(Res.string.balance_owes, listOf("$670.00")), row.balance)
            assertEquals(true, row.isTappable)
        }

    @Test
    fun aPaymentWithoutAMethodIsJustPayment() =
        runTest {
            assertEquals(UiText.Resource(Res.string.ledger_payment_title), rowOf(paymentEntry(method = null)).title)
        }

    @Test
    fun aPaymentLoggedByAnotherMemberNamesThem() =
        runTest {
            val row = rowOf(paymentEntry(loggedBy = Person.Named("Sam Rivera")))

            assertEquals(
                listOf(UiText.Resource(Res.string.ledger_payment_logged_named, listOf("Mar 2", "Sam Rivera"))),
                row.details,
            )
        }

    @Test
    fun aPaymentLoggedOnAnotherDayShowsBothDates() =
        runTest {
            val entry =
                paymentEntry(date = LocalDate(2026, 1, 18), loggedAt = Instant.parse("2026-01-24T18:00:00Z"))

            assertEquals(
                listOf(UiText.Resource(Res.string.ledger_payment_received_you, listOf("Jan 18", "Jan 24"))),
                rowOf(entry).details,
            )
        }

    @Test
    fun theLoggedDayIsTheDayInThePhonesTimeZone() =
        runTest {
            val chicago = LedgerViewModel("t1", repository, TimeZone.of("America/Chicago"))
            val entry = paymentEntry(date = LocalDate(2026, 3, 2), loggedAt = Instant.parse("2026-03-03T02:00:00Z"))
            repository.tenant.value = synced(tenantOverview(ledger = listOf(entry)))
            runCurrent()

            val content = chicago.uiState.filterIsInstance<LedgerUiState.Content>().first()

            assertEquals(
                listOf(UiText.Resource(Res.string.ledger_payment_logged_you, listOf("Mar 2"))),
                content.sections
                    .single()
                    .rows
                    .single()
                    .details,
            )
        }

    @Test
    fun balancesAreInWordsAndZeroIsPaidUp() =
        runTest {
            val credit = rowOf(paymentEntry(balance = LedgerBalance.Credit(60_000)))
            val paidUp = rowOf(paymentEntry(balance = LedgerBalance.PaidUp))

            assertEquals(UiText.Resource(Res.string.balance_credit, listOf("$600.00")), credit.balance)
            assertEquals(UiText.Resource(Res.string.balance_paid_up), paidUp.balance)
        }

    @Test
    fun aCancelledPaymentIsCrossedOutWithItsReasonAndNoBalanceAndCanBeRestored() =
        runTest {
            val cancelled =
                EntryStatus.Cancelled(
                    "Entered twice",
                    Person.Named("Sam Rivera"),
                    Instant.parse("2026-03-05T12:00:00Z"),
                )

            val row = rowOf(paymentEntry(status = cancelled, balance = null))

            assertEquals(listOf(EntryChip.Cancelled), row.chips)
            assertEquals(true, row.isStruckThrough)
            assertNull(row.balance)
            assertEquals(UiText.Plain("$1,500.00"), row.amount)
            assertEquals(
                listOf(UiText.Resource(Res.string.ledger_cancelled_named, listOf("Entered twice", "Sam Rivera"))),
                row.details,
            )
            assertEquals(true, row.canRestore)
        }

    @Test
    fun aCancelledEntryCannotBeRestoredWithoutPermission() =
        runTest {
            val cancelled = EntryStatus.Cancelled("Entered twice", Person.You, Instant.parse("2026-03-05T12:00:00Z"))

            val row = rowOf(paymentEntry(status = cancelled, balance = null, canChange = false))

            assertEquals(false, row.canRestore)
        }

    @Test
    fun aBouncedPaymentShowsWhenAndWhoAndCanBeRestored() =
        runTest {
            val bounced = EntryStatus.Bounced(Person.You, Instant.parse("2026-02-19T12:00:00Z"))

            val row = rowOf(paymentEntry(status = bounced, balance = null))

            assertEquals(listOf(EntryChip.Bounced), row.chips)
            assertEquals(false, row.isStruckThrough)
            assertEquals(listOf(UiText.Resource(Res.string.ledger_date_by_you, listOf("Feb 19"))), row.details)
            assertEquals(UiText.Plain("$1,500.00"), row.amount)
            assertEquals(true, row.canRestore)
        }

    @Test
    fun anEditedEntryShowsAnEditedChipAndWhoAndWhen() =
        runTest {
            val mine = rowOf(paymentEntry(edit = EntryEdit(Person.You, Instant.parse("2026-01-04T12:00:00Z"))))
            val theirs =
                rowOf(paymentEntry(edit = EntryEdit(Person.Named("Sam Rivera"), Instant.parse("2026-01-04T12:00:00Z"))))

            assertEquals(listOf(EntryChip.Edited), mine.chips)
            assertEquals(UiText.Resource(Res.string.ledger_edited_you, listOf("Jan 4")), mine.details.last())
            assertEquals(
                UiText.Resource(Res.string.ledger_edited_named, listOf("Jan 4", "Sam Rivera")),
                theirs.details.last(),
            )
        }

    @Test
    fun aLateFeeShowsItsMonthWhoAddedItAndTheMayNotApplyLabel() =
        runTest {
            val row =
                rowOf(
                    chargeEntry(
                        kind = ChargeKind.LateFee(YearMonth(2026, 2)),
                        addedBy = Person.Named("Sam Rivera"),
                        mayNotApply = true,
                    ),
                )

            assertEquals(UiText.Resource(Res.string.ledger_late_fee_title, listOf("February")), row.title)
            assertEquals(
                listOf(UiText.Resource(Res.string.ledger_late_fee_added_named, listOf("Sam Rivera"))),
                row.details,
            )
            assertEquals(true, row.mayNotApply)
        }

    @Test
    fun aCancelledLateFeeNoLongerSaysItMayNotApply() =
        runTest {
            val cancelled = EntryStatus.Cancelled("Mistake", Person.You, Instant.parse("2026-03-05T12:00:00Z"))

            val row =
                rowOf(
                    chargeEntry(kind = ChargeKind.LateFee(YearMonth(2026, 2)), mayNotApply = true, status = cancelled),
                )

            assertEquals(false, row.mayNotApply)
        }

    @Test
    fun aCustomDebtIsNamedByItsNote() =
        runTest {
            val row = rowOf(chargeEntry(kind = ChargeKind.Custom("Water bill, Feb")))

            assertEquals(UiText.Plain("Water bill, Feb"), row.title)
            assertEquals(listOf(UiText.Resource(Res.string.ledger_custom_added_you)), row.details)
            assertEquals(UiText.Plain("$120.00"), row.amount)
        }

    @Test
    fun anEmptyLedgerSaysWhenTheFirstRentChargeAppears() =
        runTest {
            val content =
                contentOf(
                    tenantOverview(balance = tenantBalance(nextDueDate = LocalDate(2026, 4, 15)), ledger = emptyList()),
                )

            assertEquals(
                UiText.Resource(Res.string.ledger_empty_first_rent, listOf("Apr 15, 2026")),
                content.emptyMessage,
            )
            assertEquals(emptyList(), content.sections)
        }

    @Test
    fun anEmptyLedgerWithoutANextDueDateStillExplainsItself() =
        runTest {
            val content = contentOf(tenantOverview(balance = tenantBalance(nextDueDate = null)))

            assertEquals(UiText.Resource(Res.string.ledger_empty_no_date), content.emptyMessage)
        }

    @Test
    fun logPaymentIsOfferedOnlyToCallersWhoMayLogAndOnActiveTenants() =
        runTest {
            val owner = contentOf(tenantOverview(permissions = callerPermissions(isOwner = true)))
            val archived = contentOf(tenantOverview(isArchived = true, permissions = callerPermissions(isOwner = true)))
            val none = contentOf(tenantOverview(permissions = null))

            assertEquals(listOf(true, false, false), listOf(owner, archived, none).map { it.canLogPayment })
        }

    @Test
    fun aSyncFailureKeepsTheLedgerAndShowsTheError() =
        runTest {
            repository.tenant.value =
                Synced(tenantOverview(ledger = listOf(rentEntry())), SyncStatus.Failed(TenantError.Offline))
            runCurrent()

            val content = viewModel.uiState.filterIsInstance<LedgerUiState.Content>().first()

            assertEquals(UiText.Resource(Res.string.tenant_error_offline), content.syncError)
            assertEquals(1, content.sections.size)
        }

    private suspend fun TestScope.rowOf(entry: LedgerEntry): LedgerRowUiState =
        contentOf(tenantOverview(ledger = listOf(entry)))
            .sections
            .single()
            .rows
            .single()

    private suspend fun TestScope.contentOf(overview: TenantOverview): LedgerUiState.Content {
        repository.tenant.value = synced(overview)
        runCurrent()
        return viewModel.uiState.filterIsInstance<LedgerUiState.Content>().first()
    }
}
