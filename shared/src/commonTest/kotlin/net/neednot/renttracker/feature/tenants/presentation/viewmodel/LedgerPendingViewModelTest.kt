package net.neednot.renttracker.feature.tenants.presentation.viewmodel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import net.neednot.renttracker.core.sync.Freshness
import net.neednot.renttracker.core.ui.MainDispatcherTest
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.tenants.domain.EntryStatus
import net.neednot.renttracker.feature.tenants.domain.FakePendingPaymentRepository
import net.neednot.renttracker.feature.tenants.domain.FakeTenantFreshnessRepository
import net.neednot.renttracker.feature.tenants.domain.FakeTenantRepository
import net.neednot.renttracker.feature.tenants.domain.LedgerEntry
import net.neednot.renttracker.feature.tenants.domain.Person
import net.neednot.renttracker.feature.tenants.domain.paymentEntry
import net.neednot.renttracker.feature.tenants.domain.pendingPayment
import net.neednot.renttracker.feature.tenants.domain.rentEntry
import net.neednot.renttracker.feature.tenants.domain.synced
import net.neednot.renttracker.feature.tenants.domain.tenantOverview
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerItemUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.LedgerUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.offline_banner
import net.neednot.renttracker.resources.pending_ledger_note
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class LedgerPendingViewModelTest : MainDispatcherTest() {
    private val tenants = FakeTenantRepository()
    private val pending = FakePendingPaymentRepository()
    private val freshness = FakeTenantFreshnessRepository()
    private val viewModel by lazy { LedgerViewModel("t1", tenants, freshness, pending, TimeZone.UTC) }

    @Test
    fun pendingRowsJoinTheirMonthNewestFirstWithTheNote() =
        runTest {
            pending.pendingPayments.value =
                listOf(
                    pendingPayment("march", dateReceived = LocalDate(2026, 3, 10)),
                    pendingPayment("april", dateReceived = LocalDate(2026, 4, 2)),
                )

            val content =
                contentOf(
                    paymentEntry(key = "payment:p1", date = LocalDate(2026, 3, 2)),
                    rentEntry(month = "2026-03", date = LocalDate(2026, 3, 1)),
                )

            assertEquals(listOf("April 2026", "March 2026"), content.sections.map { it.header })
            assertEquals(
                listOf(listOf("pending:april"), listOf("pending:march", "payment:p1", "rent:2026-03")),
                content.sections.map { section -> section.rows.map { it.key } },
            )
            assertEquals(UiText.Resource(Res.string.pending_ledger_note), content.pendingNote)
        }

    @Test
    fun withoutPendingRowsThereIsNoNote() =
        runTest {
            assertNull(contentOf(rentEntry()).pendingNote)
        }

    @Test
    fun aLedgerWithOnlyPendingPaymentsIsNotEmpty() =
        runTest {
            pending.pendingPayments.value = listOf(pendingPayment())

            val content = contentOf()

            assertNull(content.emptyMessage)
            assertEquals(listOf(listOf("pending:c1")), content.sections.map { section -> section.rows.map { it.key } })
        }

    @Test
    fun offlineShowsTheBannerAndHidesRestore() =
        runTest {
            freshness.ledger.value = Freshness.Offline(null)
            val cancelled = EntryStatus.Cancelled("Twice", Person.You, Instant.parse("2026-03-05T12:00:00Z"))

            val content = contentOf(paymentEntry(status = cancelled))

            assertEquals(UiText.Resource(Res.string.offline_banner), content.offlineBanner)
            assertEquals(
                listOf(false),
                content.sections
                    .flatMap { it.rows }
                    .filterIsInstance<LedgerItemUiState.Entry>()
                    .map { it.row.canRestore },
            )
        }

    @Test
    fun onlineThereIsNoOfflineBanner() =
        runTest {
            assertNull(contentOf(rentEntry()).offlineBanner)
        }

    private suspend fun TestScope.contentOf(vararg entries: LedgerEntry): LedgerUiState.Content {
        tenants.tenant.value = synced(tenantOverview(ledger = entries.toList()))
        runCurrent()
        return viewModel.uiState.filterIsInstance<LedgerUiState.Content>().first()
    }
}
