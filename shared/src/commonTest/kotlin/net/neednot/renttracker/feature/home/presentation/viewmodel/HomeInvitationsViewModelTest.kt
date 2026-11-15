package net.neednot.renttracker.feature.home.presentation.viewmodel

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.github.michaelbull.result.Err
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.neednot.renttracker.core.access.MemberPermissionLabel
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.ui.MainDispatcherTest
import net.neednot.renttracker.core.ui.RecordingMessageDispatcher
import net.neednot.renttracker.core.ui.UiMessage
import net.neednot.renttracker.core.ui.UiMessageVariant
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.home.domain.FakeHomeRepository
import net.neednot.renttracker.feature.home.domain.HomeError
import net.neednot.renttracker.feature.home.domain.HomeInvitation
import net.neednot.renttracker.feature.home.domain.HomeOverview
import net.neednot.renttracker.feature.home.presentation.uistate.DeclineInviteUiState
import net.neednot.renttracker.feature.home.presentation.uistate.HomeUiState
import net.neednot.renttracker.feature.home.presentation.uistate.InvitationCardUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.home_archived_groups_row
import net.neednot.renttracker.resources.home_decline_message
import net.neednot.renttracker.resources.home_decline_title
import net.neednot.renttracker.resources.home_invitation_as_bookkeeper
import net.neednot.renttracker.resources.home_invitation_as_custom
import net.neednot.renttracker.resources.home_invitation_as_manager
import net.neednot.renttracker.resources.home_invitation_as_viewer
import net.neednot.renttracker.resources.home_invitation_description
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HomeInvitationsViewModelTest : MainDispatcherTest() {
    private val repository = FakeHomeRepository()
    private val messages = RecordingMessageDispatcher()
    private val viewModel by lazy { HomeViewModel(repository, messages) }

    private val oakAve = invitation("i1", "Oak Ave Duplex", MemberPermissionLabel.Bookkeeper)

    private fun invitation(
        inviteId: String,
        groupName: String,
        access: MemberPermissionLabel,
    ) = HomeInvitation(inviteId = inviteId, groupName = groupName, invitedByName = "Sam Rivera", access = access)

    private fun show(
        invitations: List<HomeInvitation>,
        archivedGroupCount: Int = 0,
    ) {
        repository.home.value =
            Synced(HomeOverview(emptyList(), invitations, archivedGroupCount), SyncStatus.Synced)
    }

    private suspend fun ReceiveTurbine<HomeUiState>.awaitContent(): HomeUiState.Content =
        awaitItem() as HomeUiState.Content

    @Test
    fun anInvitationShowsTheGroupWhoInvitedYouAndTheAccessAsAPresetName() =
        runTest {
            show(listOf(oakAve))

            viewModel.uiState.test {
                skipItems(1)

                assertEquals(
                    InvitationCardUiState(
                        inviteId = "i1",
                        groupName = "Oak Ave Duplex",
                        summary = UiText.Resource(Res.string.home_invitation_as_bookkeeper, listOf("Sam Rivera")),
                        description = UiText.Resource(Res.string.home_invitation_description, listOf("Oak Ave Duplex")),
                        isWorking = false,
                    ),
                    awaitContent().invitations.single(),
                )
            }
        }

    @Test
    fun theAccessNamesFollowThePreset() =
        runTest {
            show(
                listOf(
                    invitation("i1", "A", MemberPermissionLabel.Viewer),
                    invitation("i2", "B", MemberPermissionLabel.Manager),
                    invitation("i3", "C", MemberPermissionLabel.Custom),
                ),
            )

            viewModel.uiState.test {
                skipItems(1)

                assertEquals(
                    listOf(
                        Res.string.home_invitation_as_viewer,
                        Res.string.home_invitation_as_manager,
                        Res.string.home_invitation_as_custom,
                    ),
                    awaitContent().invitations.map { (it.summary as UiText.Resource).resource },
                )
            }
        }

    @Test
    fun withInvitationsAndNoGroupsTheInvitationsStillShow() =
        runTest {
            show(listOf(oakAve))

            viewModel.uiState.test {
                skipItems(1)

                val content = awaitContent()
                assertTrue(content.groups.isEmpty())
                assertEquals(1, content.invitations.size)
            }
        }

    @Test
    fun withNoInvitationsThereIsNoInvitationsSection() =
        runTest {
            repository.home.value =
                Synced(HomeOverview(emptyList(), emptyList(), archivedGroupCount = 1), SyncStatus.Synced)

            viewModel.uiState.test {
                skipItems(1)

                assertTrue(awaitContent().invitations.isEmpty())
            }
        }

    @Test
    fun theArchivedGroupsRowShowsTheCountAndIsHiddenWithoutArchivedGroups() =
        runTest {
            show(listOf(oakAve), archivedGroupCount = 2)

            viewModel.uiState.test {
                skipItems(1)
                assertEquals(
                    UiText.Resource(Res.string.home_archived_groups_row, listOf(2)),
                    awaitContent().archivedGroupsRow,
                )

                show(listOf(oakAve), archivedGroupCount = 0)

                assertNull(awaitContent().archivedGroupsRow)
            }
        }

    @Test
    fun acceptingSendsTheInviteIdAndShowsNoMessage() =
        runTest {
            show(listOf(oakAve))

            viewModel.uiState.test {
                skipItems(2)

                viewModel.onAcceptClicked("i1")
                cancelAndIgnoreRemainingEvents()
            }
            runCurrent()

            assertEquals(listOf("i1"), repository.acceptedInviteIds)
            assertEquals(emptyList<Any>(), messages.shown)
        }

    @Test
    fun aMissingInviteIsShownInTheSnackbar() =
        runTest {
            show(listOf(oakAve))
            repository.invitationResult = Err(HomeError.Rejected("This invite no longer exists."))

            viewModel.uiState.test {
                skipItems(2)

                viewModel.onAcceptClicked("i1")
                cancelAndIgnoreRemainingEvents()
            }
            runCurrent()

            assertEquals(
                listOf(UiMessage(UiText.Plain("This invite no longer exists."), UiMessageVariant.Error)),
                messages.shown,
            )
        }

    @Test
    fun decliningAsksFirstNamingTheGroupAndWhoInvitedYou() =
        runTest {
            show(listOf(oakAve))

            viewModel.uiState.test {
                skipItems(2)

                viewModel.onDeclineClicked("i1")

                assertEquals(
                    DeclineInviteUiState(
                        inviteId = "i1",
                        title = UiText.Resource(Res.string.home_decline_title, listOf("Oak Ave Duplex")),
                        message = UiText.Resource(Res.string.home_decline_message, listOf("Sam Rivera")),
                    ),
                    awaitContent().declineDialog,
                )
            }
            assertEquals(emptyList<Any>(), repository.declinedInviteIds)
        }

    @Test
    fun cancellingTheDeclineDialogDeclinesNothing() =
        runTest {
            show(listOf(oakAve))

            viewModel.uiState.test {
                skipItems(2)
                viewModel.onDeclineClicked("i1")
                skipItems(1)

                viewModel.onDeclineDismissed()

                assertNull(awaitContent().declineDialog)
            }
            assertEquals(emptyList<Any>(), repository.declinedInviteIds)
        }

    @Test
    fun confirmingDeclinesTheInviteAndClosesTheDialog() =
        runTest {
            show(listOf(oakAve))

            viewModel.uiState.test {
                skipItems(2)
                viewModel.onDeclineClicked("i1")
                skipItems(1)

                viewModel.onDeclineConfirmed()

                assertNull(awaitContent().declineDialog)
                cancelAndIgnoreRemainingEvents()
            }
            runCurrent()
            assertEquals(listOf("i1"), repository.declinedInviteIds)
            assertEquals(emptyList<Any>(), repository.acceptedInviteIds)
        }

    @Test
    fun aFailedDeclineIsShownInTheSnackbar() =
        runTest {
            show(listOf(oakAve))
            repository.invitationResult = Err(HomeError.Offline)

            viewModel.uiState.test {
                skipItems(2)
                viewModel.onDeclineClicked("i1")
                skipItems(1)

                viewModel.onDeclineConfirmed()
                cancelAndIgnoreRemainingEvents()
            }
            runCurrent()

            assertEquals(1, messages.shown.size)
            assertEquals(UiMessageVariant.Error, messages.shown.single().variant)
        }
}
