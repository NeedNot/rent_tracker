package net.neednot.renttracker.feature.home.presentation.viewmodel

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import net.neednot.renttracker.core.access.MemberPermissionLabel
import net.neednot.renttracker.core.sync.Freshness
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.ui.MainDispatcherTest
import net.neednot.renttracker.core.ui.RecordingMessageDispatcher
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.home.domain.FakeHomeAccountRepository
import net.neednot.renttracker.feature.home.domain.FakeHomeOfflineRepository
import net.neednot.renttracker.feature.home.domain.FakeHomeRepository
import net.neednot.renttracker.feature.home.domain.GroupOverview
import net.neednot.renttracker.feature.home.domain.HomeInvitation
import net.neednot.renttracker.feature.home.domain.HomeOverview
import net.neednot.renttracker.feature.home.domain.RejectedPayment
import net.neednot.renttracker.feature.home.domain.RejectedPaymentReason
import net.neednot.renttracker.feature.home.presentation.uistate.HomeUiState
import net.neednot.renttracker.feature.home.presentation.uistate.RejectedPaymentUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.home_archived_groups_row
import net.neednot.renttracker.resources.home_rejected_payment_title
import net.neednot.renttracker.resources.home_rejected_payments_summary
import net.neednot.renttracker.resources.offline_banner_hours
import net.neednot.renttracker.resources.pending_rejected_archived
import net.neednot.renttracker.resources.pending_rejected_moved
import net.neednot.renttracker.resources.pending_rejected_no_permission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

@OptIn(ExperimentalCoroutinesApi::class)
class HomeOfflineViewModelTest : MainDispatcherTest() {
    private val repository = FakeHomeRepository()
    private val offlineRepository = FakeHomeOfflineRepository()
    private val accountRepository = FakeHomeAccountRepository()
    private val viewModel by lazy {
        HomeViewModel(repository, offlineRepository, accountRepository, RecordingMessageDispatcher())
    }

    private val mapleCourt =
        GroupOverview(
            groupId = "g2",
            name = "Maple Court",
            totalOwedCents = 0,
            activeTenantCount = 1,
            lateTenantCount = 0,
            archivedOwedCents = 0,
        )
    private val oakAve =
        HomeInvitation("i1", "Oak Ave Duplex", invitedByName = "Sam Rivera", access = MemberPermissionLabel.Manager)

    private fun showGroupAndInvitation() {
        repository.home.value =
            Synced(HomeOverview(listOf(mapleCourt), listOf(oakAve), archivedGroupCount = 1), SyncStatus.Synced)
    }

    private fun rejected(
        clientId: String,
        tenantName: String = "Apt 3B",
        reason: RejectedPaymentReason = RejectedPaymentReason.NoPermission,
        message: String? = null,
    ) = RejectedPayment(
        clientId = clientId,
        tenantName = tenantName,
        amountCents = 55_000,
        dateReceived = LocalDate(2026, 3, 2),
        reason = reason,
        message = message,
    )

    private suspend fun ReceiveTurbine<HomeUiState>.awaitContent(): HomeUiState.Content =
        awaitItem() as HomeUiState.Content

    @Test
    fun offlineShowsTheBannerWithHowOldTheCacheIs() =
        runTest {
            showGroupAndInvitation()
            offlineRepository.freshness.value = Freshness.Offline(2.hours)

            viewModel.uiState.test {
                skipItems(1)

                assertEquals(UiText.Resource(Res.string.offline_banner_hours, listOf(2L)), awaitContent().offlineBanner)
            }
        }

    @Test
    fun onlineShowsNoBanner() =
        runTest {
            showGroupAndInvitation()

            viewModel.uiState.test {
                skipItems(1)

                assertNull(awaitContent().offlineBanner)
            }
        }

    @Test
    fun offlineHidesTheCreateGroupActionAndTheInvitations() =
        runTest {
            showGroupAndInvitation()
            offlineRepository.freshness.value = Freshness.Offline(null)

            viewModel.uiState.test {
                skipItems(1)

                val content = awaitContent()
                assertFalse(content.canCreateGroup)
                assertEquals(emptyList(), content.invitations)
                assertEquals(listOf("g2"), content.groups.map { it.groupId })
                assertEquals(UiText.Resource(Res.string.home_archived_groups_row, listOf(1)), content.archivedGroupsRow)
            }
        }

    @Test
    fun onlineShowsTheCreateGroupActionAndTheInvitations() =
        runTest {
            showGroupAndInvitation()

            viewModel.uiState.test {
                skipItems(1)

                val content = awaitContent()
                assertTrue(content.canCreateGroup)
                assertEquals(listOf("i1"), content.invitations.map { it.inviteId })
            }
        }

    @Test
    fun withNoRejectedPaymentsThereIsNoCouldntSyncCard() =
        runTest {
            showGroupAndInvitation()

            viewModel.uiState.test {
                skipItems(1)

                assertNull(awaitContent().rejectedPayments)
            }
        }

    @Test
    fun theCouldntSyncCardCountsTheRejectedPayments() =
        runTest {
            showGroupAndInvitation()
            offlineRepository.rejectedPayments.value = listOf(rejected("c1"), rejected("c2"))

            viewModel.uiState.test {
                skipItems(1)

                assertEquals(
                    UiText.Plural(Res.plurals.home_rejected_payments_summary, 2, listOf(2)),
                    awaitContent().rejectedPayments?.summary,
                )
            }
        }

    @Test
    fun theCouldntSyncCardShowsWhileOffline() =
        runTest {
            showGroupAndInvitation()
            offlineRepository.freshness.value = Freshness.Offline(null)
            offlineRepository.rejectedPayments.value = listOf(rejected("c1"))

            viewModel.uiState.test {
                skipItems(1)

                assertEquals(
                    UiText.Plural(Res.plurals.home_rejected_payments_summary, 1, listOf(1)),
                    awaitContent().rejectedPayments?.summary,
                )
            }
        }

    @Test
    fun theCouldntSyncCardShowsEvenWithNoGroups() =
        runTest {
            repository.home.value = Synced(HomeOverview(emptyList(), emptyList(), 0), SyncStatus.Synced)
            offlineRepository.rejectedPayments.value = listOf(rejected("c1"))

            viewModel.uiState.test {
                skipItems(1)

                assertEquals(1, (awaitItem() as HomeUiState.Empty).rejectedPayments?.payments?.size)
            }
        }

    @Test
    fun openingTheCardListsEachPaymentWithTenantAmountDateAndReason() =
        runTest {
            showGroupAndInvitation()
            offlineRepository.rejectedPayments.value =
                listOf(
                    rejected("c1", tenantName = "Apt 3B", reason = RejectedPaymentReason.TenantMoved),
                    rejected("c2", tenantName = "Apt 4A", reason = RejectedPaymentReason.TenantArchived),
                    rejected("c3", tenantName = "Apt 5C", reason = RejectedPaymentReason.Other, message = "Too large."),
                )

            viewModel.uiState.test {
                skipItems(1)
                awaitItem()

                viewModel.onRejectedPaymentsClicked()

                val sheet = checkNotNull(awaitContent().rejectedPayments)
                assertTrue(sheet.isSheetOpen)
                assertEquals(
                    listOf(
                        paymentUiState("c1", "Apt 3B", UiText.Resource(Res.string.pending_rejected_moved)),
                        paymentUiState("c2", "Apt 4A", UiText.Resource(Res.string.pending_rejected_archived)),
                        paymentUiState("c3", "Apt 5C", UiText.Plain("Too large.")),
                    ),
                    sheet.payments,
                )
            }
        }

    @Test
    fun anAccessRejectionExplainsThePermissionWasLost() =
        runTest {
            showGroupAndInvitation()
            offlineRepository.rejectedPayments.value = listOf(rejected("c1"))

            viewModel.uiState.test {
                skipItems(1)

                val payment = awaitContent().rejectedPayments?.payments?.single()
                assertEquals(UiText.Resource(Res.string.pending_rejected_no_permission), payment?.reason)
            }
        }

    @Test
    fun closingTheSheetKeepsTheCard() =
        runTest {
            showGroupAndInvitation()
            offlineRepository.rejectedPayments.value = listOf(rejected("c1"))

            viewModel.uiState.test {
                skipItems(1)
                awaitItem()
                viewModel.onRejectedPaymentsClicked()
                awaitItem()

                viewModel.onRejectedPaymentsDismissed()

                val rejectedPayments = awaitContent().rejectedPayments
                assertFalse(rejectedPayments?.isSheetOpen == true)
                assertEquals(1, rejectedPayments?.payments?.size)
            }
        }

    @Test
    fun dismissingAPaymentDiscardsItThroughTheRepository() =
        runTest {
            showGroupAndInvitation()
            offlineRepository.rejectedPayments.value = listOf(rejected("c1"), rejected("c2"))

            viewModel.uiState.test {
                skipItems(1)
                awaitItem()

                viewModel.onRejectedPaymentDismissed("c2")
                cancelAndIgnoreRemainingEvents()
            }
            runCurrent()

            assertEquals(listOf("c2"), offlineRepository.dismissedClientIds)
        }

    private fun paymentUiState(
        clientId: String,
        tenantName: String,
        reason: UiText,
    ) = RejectedPaymentUiState(
        clientId = clientId,
        title = UiText.Resource(Res.string.home_rejected_payment_title, listOf(tenantName)),
        amount = "$550.00",
        dateReceived = "Mar 2, 2026",
        reason = reason,
    )
}
