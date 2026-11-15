package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import net.neednot.renttracker.core.sync.Freshness
import net.neednot.renttracker.core.ui.MainDispatcherTest
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.tenants.domain.EntryStatus
import net.neednot.renttracker.feature.tenants.domain.FakePendingPaymentRepository
import net.neednot.renttracker.feature.tenants.domain.FakeTenantCalendarRepository
import net.neednot.renttracker.feature.tenants.domain.FakeTenantFreshnessRepository
import net.neednot.renttracker.feature.tenants.domain.FakeTenantRepository
import net.neednot.renttracker.feature.tenants.domain.LateFeeSuggestion
import net.neednot.renttracker.feature.tenants.domain.PendingRejection
import net.neednot.renttracker.feature.tenants.domain.PendingState
import net.neednot.renttracker.feature.tenants.domain.Person
import net.neednot.renttracker.feature.tenants.domain.TenantOverview
import net.neednot.renttracker.feature.tenants.domain.paymentEntry
import net.neednot.renttracker.feature.tenants.domain.pendingPayment
import net.neednot.renttracker.feature.tenants.domain.rentEntry
import net.neednot.renttracker.feature.tenants.domain.synced
import net.neednot.renttracker.feature.tenants.domain.tenantOverview
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerItemUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingBannerUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingChip
import net.neednot.renttracker.feature.tenants.presentation.uistate.PendingRowUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ledger_amount_paid
import net.neednot.renttracker.resources.ledger_payment_title_zelle
import net.neednot.renttracker.resources.offline_banner_minutes
import net.neednot.renttracker.resources.pending_banner_attention
import net.neednot.renttracker.resources.pending_banner_waiting
import net.neednot.renttracker.resources.pending_banner_waiting_subtitle
import net.neednot.renttracker.resources.pending_row_not_saved
import net.neednot.renttracker.resources.pending_row_possible_duplicate
import net.neednot.renttracker.resources.pending_row_saved_on_phone
import net.neednot.renttracker.resources.pending_row_tenant_changed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class TenantPendingViewModelTest : MainDispatcherTest() {
    private val tenants = FakeTenantRepository()
    private val pending = FakePendingPaymentRepository()
    private val freshness = FakeTenantFreshnessRepository()
    private val viewModel by lazy {
        TenantViewModel(
            tenantId = "t1",
            tenantRepository = tenants,
            calendarRepository = FakeTenantCalendarRepository(),
            freshnessRepository = freshness,
            pendingPaymentRepository = pending,
            timeZone = TimeZone.UTC,
        )
    }

    private val suggestion = LateFeeSuggestion(month = YearMonth(2026, 3), cents = 5_000)

    @Test
    fun onlineThereIsNoOfflineBannerAndEveryActionShows() =
        runTest {
            val content = contentOf(tenantOverview(lateFeeSuggestion = suggestion))

            assertNull(content.offlineBanner)
            assertTrue(content.canLogPayment && content.canAddCharge && content.hasMenu && content.canEditDetails)
            assertEquals(suggestion.month.toString(), content.lateFeeSuggestion?.month)
        }

    @Test
    fun offlineShowsTheBannerAndOnlyLogPaymentStays() =
        runTest {
            freshness.tenant.value = Freshness.Offline(40.minutes)

            val content = contentOf(tenantOverview(lateFeeSuggestion = suggestion))

            assertEquals(UiText.Resource(Res.string.offline_banner_minutes, listOf(40L)), content.offlineBanner)
            assertTrue(content.canLogPayment)
            assertFalse(content.canAddCharge)
            assertFalse(content.hasMenu)
            assertFalse(content.canEditDetails)
            assertNull(content.lateFeeSuggestion)
        }

    @Test
    fun offlineLedgerPreviewRowsOfferNoRestore() =
        runTest {
            freshness.tenant.value = Freshness.Offline(null)
            val cancelled = EntryStatus.Cancelled("Twice", Person.You, Instant.parse("2026-03-05T12:00:00Z"))

            val content = contentOf(tenantOverview(ledger = listOf(paymentEntry(status = cancelled))))

            assertEquals(listOf(false), content.ledgerPreview.entryRows().map { it.canRestore })
        }

    @Test
    fun onePendingPaymentShowsTheWaitingBanner() =
        runTest {
            pending.pendingPayments.value = listOf(pendingPayment())

            val content = contentOf(tenantOverview())

            assertEquals(
                PendingBannerUiState.Waiting(
                    title = UiText.Plural(Res.plurals.pending_banner_waiting, 1, listOf(1)),
                    subtitle = UiText.Resource(Res.string.pending_banner_waiting_subtitle),
                ),
                content.pendingBanner,
            )
        }

    @Test
    fun theWaitingBannerCountsEveryPendingPayment() =
        runTest {
            pending.pendingPayments.value = listOf(pendingPayment("c1"), pendingPayment("c2"))

            val banner = contentOf(tenantOverview()).pendingBanner

            assertEquals(UiText.Plural(Res.plurals.pending_banner_waiting, 2, listOf(2)), banner?.title)
        }

    @Test
    fun theAttentionBannerWinsAndReviewsTheFirstPaymentNeedingAttention() =
        runTest {
            pending.pendingPayments.value =
                listOf(
                    pendingPayment("c1"),
                    pendingPayment("c2", state = PendingState.PossibleDuplicate),
                    pendingPayment("c3", state = PendingState.Rejected(PendingRejection.TenantArchived)),
                )

            val banner = contentOf(tenantOverview()).pendingBanner

            assertEquals(
                PendingBannerUiState.Attention(
                    title = UiText.Plural(Res.plurals.pending_banner_attention, 2, listOf(2)),
                    reviewClientId = "c2",
                ),
                banner,
            )
        }

    @Test
    fun noPendingPaymentsMeansNoBanner() =
        runTest {
            assertNull(contentOf(tenantOverview()).pendingBanner)
        }

    @Test
    fun aPendingRowIsGreyedPaidWithoutABalanceAndLabelledByItsState() =
        runTest {
            val cases =
                listOf(
                    PendingState.Waiting to (PendingChip.Pending to Res.string.pending_row_saved_on_phone),
                    PendingState.PossibleDuplicate to
                        (PendingChip.NeedsConfirmation to Res.string.pending_row_possible_duplicate),
                    PendingState.TenantChanged to
                        (PendingChip.NeedsConfirmation to Res.string.pending_row_tenant_changed),
                    PendingState.Rejected(PendingRejection.NoPermission) to
                        (PendingChip.Rejected to Res.string.pending_row_not_saved),
                )

            cases.forEach { (state, expected) ->
                pending.pendingPayments.value = listOf(pendingPayment(state = state))

                assertEquals(
                    PendingRowUiState(
                        clientId = "c1",
                        date = "Mar 10",
                        title = UiText.Resource(Res.string.ledger_payment_title_zelle),
                        chip = expected.first,
                        detail = UiText.Resource(expected.second),
                        amount = UiText.Resource(Res.string.ledger_amount_paid, listOf("$550.00")),
                    ),
                    contentOf(tenantOverview()).ledgerPreview.pendingRows().single(),
                )
            }
        }

    @Test
    fun pendingRowsSitByDateReceivedAboveTheThreePreviewEntries() =
        runTest {
            pending.pendingPayments.value =
                listOf(
                    pendingPayment("newest", dateReceived = LocalDate(2026, 3, 10)),
                    pendingPayment("middle", dateReceived = LocalDate(2026, 2, 20)),
                )
            val ledger =
                listOf(
                    paymentEntry(key = "payment:p1", date = LocalDate(2026, 3, 2)),
                    rentEntry(month = "2026-03", date = LocalDate(2026, 3, 1)),
                    rentEntry(month = "2026-02", date = LocalDate(2026, 2, 1)),
                    rentEntry(month = "2026-01", date = LocalDate(2026, 1, 1)),
                )

            val content = contentOf(tenantOverview(ledger = ledger))

            assertEquals(
                listOf("pending:newest", "payment:p1", "rent:2026-03", "pending:middle", "rent:2026-02"),
                content.ledgerPreview.map { it.key },
            )
        }

    @Test
    fun pendingPaymentsNeverChangeTheBalance() =
        runTest {
            val withoutPending = contentOf(tenantOverview()).balance
            pending.pendingPayments.value = listOf(pendingPayment())

            assertEquals(withoutPending, contentOf(tenantOverview()).balance)
        }

    private fun List<LedgerItemUiState>.entryRows() = filterIsInstance<LedgerItemUiState.Entry>().map { it.row }

    private fun List<LedgerItemUiState>.pendingRows() = filterIsInstance<LedgerItemUiState.Pending>().map { it.row }

    private suspend fun TestScope.contentOf(overview: TenantOverview): TenantUiState.Content {
        tenants.tenant.value = synced(overview)
        runCurrent()
        return viewModel.uiState.filterIsInstance<TenantUiState.Content>().first()
    }
}
