package net.neednot.renttracker.feature.group.presentation.viewmodel

import app.cash.turbine.test
import com.github.michaelbull.result.Err
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.ui.MainDispatcherTest
import net.neednot.renttracker.core.ui.RecordingMessageDispatcher
import net.neednot.renttracker.core.ui.UiMessage
import net.neednot.renttracker.core.ui.UiMessageVariant
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.group.domain.FakeGroupFreshnessRepository
import net.neednot.renttracker.feature.group.domain.FakeGroupRepository
import net.neednot.renttracker.feature.group.domain.GroupError
import net.neednot.renttracker.feature.group.domain.groupDetails
import net.neednot.renttracker.feature.group.domain.groupTenant
import net.neednot.renttracker.feature.group.presentation.uistate.GroupDialogUiState
import net.neednot.renttracker.feature.group.presentation.uistate.GroupUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.balance_owes
import net.neednot.renttracker.resources.group_archive_tenant_count
import net.neednot.renttracker.resources.group_error_offline
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GroupViewModelActionsTest : MainDispatcherTest() {
    private val repository = FakeGroupRepository()
    private val freshnessRepository = FakeGroupFreshnessRepository()
    private val dispatcher = RecordingMessageDispatcher()
    private val viewModel by lazy { GroupViewModel("g1", repository, freshnessRepository, dispatcher, TimeZone.UTC) }

    private val tenants = listOf(groupTenant("t1"), groupTenant("t2"))

    private fun showGroup() {
        repository.group.value =
            Synced(groupDetails(activeTenants = tenants, totalOwedCents = 390_000), SyncStatus.Synced)
    }

    private fun GroupUiState.dialog() = (this as GroupUiState.Content).chrome.dialog

    @Test
    fun archiveAsksForConfirmationShowingTheTenantCountAndWhatIsOwed() =
        runTest {
            showGroup()
            viewModel.uiState.test {
                skipItems(1)

                viewModel.onArchiveClicked()

                val expected =
                    GroupDialogUiState.Archive(
                        groupName = "1st Street",
                        tenantCount = UiText.Plural(Res.plurals.group_archive_tenant_count, 2, listOf(2)),
                        balance = UiText.Resource(Res.string.balance_owes, listOf("$3,900.00")),
                    )
                assertEquals(expected, awaitItem().dialog())
            }
        }

    @Test
    fun cancellingTheArchiveDialogArchivesNothing() =
        runTest {
            showGroup()
            viewModel.uiState.test {
                skipItems(1)
                viewModel.onArchiveClicked()
                skipItems(1)

                viewModel.onDialogDismissed()

                assertNull(awaitItem().dialog())
            }
            assertEquals(emptyList(), repository.archivedGroupIds)
        }

    @Test
    fun confirmingArchiveArchivesTheGroupAndReturnsHome() =
        runTest {
            showGroup()
            viewModel.uiState.test {
                skipItems(1)
                viewModel.onArchiveClicked()
                skipItems(1)

                viewModel.onArchiveConfirmed()

                viewModel.events.test { assertEquals(GroupEvent.ReturnHome, awaitItem()) }
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(listOf("g1"), repository.archivedGroupIds)
        }

    @Test
    fun aFailedArchiveClosesTheDialogAndShowsTheError() =
        runTest {
            showGroup()
            repository.archiveResult = Err(GroupError.Offline)
            viewModel.uiState.test {
                skipItems(1)
                viewModel.onArchiveClicked()
                skipItems(1)

                viewModel.onArchiveConfirmed()
                runCurrent()

                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(
                listOf(UiMessage(UiText.Resource(Res.string.group_error_offline), UiMessageVariant.Error)),
                dispatcher.shown,
            )
            assertNull(viewModel.uiState.value.let { (it as? GroupUiState.Content)?.chrome?.dialog })
        }

    @Test
    fun confirmingLeaveLeavesTheGroupAndReturnsHomeWithoutAMessage() =
        runTest {
            showGroup()
            viewModel.uiState.test {
                skipItems(1)
                viewModel.onLeaveClicked()
                assertTrue(awaitItem().dialog() is GroupDialogUiState.Leave)

                viewModel.onLeaveConfirmed()

                viewModel.events.test { assertEquals(GroupEvent.ReturnHome, awaitItem()) }
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(listOf("g1"), repository.leftGroupIds)
            assertEquals(emptyList(), dispatcher.shown)
        }

    @Test
    fun theOwnerLeaveRefusalShowsTheTransferOwnershipDialog() =
        runTest {
            showGroup()
            repository.leaveResult = Err(GroupError.ValidationFailed("Transfer ownership before leaving."))
            viewModel.uiState.test {
                skipItems(1)
                viewModel.onLeaveClicked()
                skipItems(1)

                viewModel.onLeaveConfirmed()

                assertEquals(GroupDialogUiState.LeaveOwner("1st Street"), expectDialog())
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(emptyList(), dispatcher.shown)
        }

    @Test
    fun transferOwnershipFromTheOwnerDialogOpensTheTransferScreen() =
        runTest {
            showGroup()
            viewModel.uiState.test {
                skipItems(1)

                viewModel.onTransferOwnershipClicked()

                viewModel.events.test { assertEquals(GroupEvent.OpenTransferOwnership, awaitItem()) }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun unarchivingCallsTheServerAndLeavesTheScreenToUpdateItself() =
        runTest {
            showGroup()
            viewModel.uiState.test {
                skipItems(1)

                viewModel.onUnarchiveClicked()
                runCurrent()

                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(listOf("g1"), repository.unarchivedGroupIds)
            assertEquals(emptyList(), dispatcher.shown)
        }

    @Test
    fun aSecondConfirmWhileTheFirstIsRunningIsIgnored() =
        runTest {
            showGroup()
            viewModel.uiState.test {
                skipItems(1)
                viewModel.onArchiveClicked()
                skipItems(1)

                viewModel.onArchiveConfirmed()
                viewModel.onArchiveConfirmed()
                runCurrent()

                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(listOf("g1"), repository.archivedGroupIds)
        }

    private suspend fun app.cash.turbine.ReceiveTurbine<GroupUiState>.expectDialog(): GroupDialogUiState? {
        var dialog = awaitItem().dialog()
        while (dialog !is GroupDialogUiState.LeaveOwner) dialog = awaitItem().dialog()
        return dialog
    }
}
