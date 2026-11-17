package net.neednot.renttracker.feature.members.presentation.viewmodel

import com.github.michaelbull.result.Err
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import net.neednot.renttracker.core.access.MemberPermissionLabel
import net.neednot.renttracker.core.data.MembershipRole
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.ui.MainDispatcherTest
import net.neednot.renttracker.core.ui.RecordingMessageDispatcher
import net.neednot.renttracker.core.ui.UiMessage
import net.neednot.renttracker.core.ui.UiMessageVariant
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.members.domain.FakeMembersRepository
import net.neednot.renttracker.feature.members.domain.Member
import net.neednot.renttracker.feature.members.domain.MembersError
import net.neednot.renttracker.feature.members.domain.MembersRepositoryCall
import net.neednot.renttracker.feature.members.domain.jen
import net.neednot.renttracker.feature.members.domain.kevin
import net.neednot.renttracker.feature.members.domain.maria
import net.neednot.renttracker.feature.members.domain.needNot
import net.neednot.renttracker.feature.members.domain.overviewSeenBy
import net.neednot.renttracker.feature.members.domain.sam
import net.neednot.renttracker.feature.members.domain.synced
import net.neednot.renttracker.feature.members.presentation.uistate.InviteRowUiState
import net.neednot.renttracker.feature.members.presentation.uistate.MemberRowAction
import net.neednot.renttracker.feature.members.presentation.uistate.MemberRowUiState
import net.neednot.renttracker.feature.members.presentation.uistate.MembersUiState
import net.neednot.renttracker.feature.members.presentation.uistate.RemoveMemberDialogUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MembersViewModelTest : MainDispatcherTest() {
    private val repository = FakeMembersRepository()
    private val messages = RecordingMessageDispatcher()

    private fun TestScope.openMembers(seenBy: Member): MembersViewModel {
        repository.members.value = synced(overviewSeenBy(seenBy))
        val viewModel = MembersViewModel("g1", repository, messages, TimeZone.of("America/Chicago"))
        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()
        return viewModel
    }

    private fun MembersViewModel.content(): MembersUiState.Content = uiState.value as MembersUiState.Content

    @Test
    fun theOwnerOpensEveryOtherMembersPermissionsAndCanInviteRevokeAndTransfer() =
        runTest {
            val content = openMembers(seenBy = needNot).content()

            assertEquals(
                MemberRowUiState(
                    "m1",
                    "N",
                    "Need_Not",
                    "owner@example.com",
                    true,
                    MemberPermissionLabel.Owner,
                    MemberRowAction.None,
                ),
                content.members.first(),
            )
            assertEquals(
                listOf(MemberRowAction.OpenPermissions),
                content.members
                    .drop(1)
                    .map { it.action }
                    .distinct(),
            )
            assertEquals(
                listOf(
                    MemberPermissionLabel.Manager,
                    MemberPermissionLabel.Custom,
                    MemberPermissionLabel.Bookkeeper,
                    MemberPermissionLabel.Viewer,
                ),
                content.members.drop(1).map { it.label },
            )
            assertEquals(
                listOf(
                    InviteRowUiState(
                        "i1",
                        "alex.m@example.com",
                        MemberPermissionLabel.Bookkeeper,
                        "Mar 16",
                        canRevoke = true,
                    ),
                ),
                content.invites,
            )
            assertTrue(content.canInvite)
            assertTrue(content.canTransferOwnership)
            assertFalse(content.showsViewOnlyInfo)
        }

    @Test
    fun aViewerSeesEveryoneReadOnlyWithTheInfoLineAndNoActions() =
        runTest {
            val content = openMembers(seenBy = jen).content()

            assertTrue(content.showsViewOnlyInfo)
            assertEquals(listOf(MemberRowAction.None), content.members.map { it.action }.distinct())
            assertEquals(listOf("m5"), content.members.filter { it.isYou }.map { it.membershipId })
            assertFalse(content.invites.single().canRevoke)
            assertFalse(content.canInvite)
            assertFalse(content.canTransferOwnership)
        }

    @Test
    fun anAddAndEditMemberCanRemoveOnlyMembersAtOrBelowTheirOwnPermissions() =
        runTest {
            val content = openMembers(seenBy = kevin).content()

            assertEquals(
                listOf(
                    MemberRowAction.None,
                    MemberRowAction.None,
                    MemberRowAction.None,
                    MemberRowAction.Remove,
                    MemberRowAction.Remove,
                ),
                content.members.map { it.action },
            )
            assertTrue(content.canInvite)
            assertTrue(content.invites.single().canRevoke)
            assertFalse(content.canTransferOwnership)
            assertFalse(content.showsViewOnlyInfo)
        }

    @Test
    fun anArchivedGroupIsReadOnlyEvenForTheOwner() =
        runTest {
            val archived = overviewSeenBy(needNot).let { it.copy(access = it.access.copy(groupArchived = true)) }
            repository.members.value = synced(archived)
            val viewModel = MembersViewModel("g1", repository, messages)
            backgroundScope.launch { viewModel.uiState.collect {} }
            runCurrent()

            val content = viewModel.content()
            assertFalse(content.canInvite)
            assertFalse(content.canTransferOwnership)
            assertEquals(listOf(MemberRowAction.None), content.members.map { it.action }.distinct())
        }

    @Test
    fun removingAsksFirstThenRemovesAndClosesTheDialog() =
        runTest {
            val viewModel = openMembers(seenBy = kevin)

            viewModel.onRemoveClicked(maria.membershipId)
            runCurrent()
            assertEquals(
                RemoveMemberDialogUiState("m4", "Maria Lopez", "Maria"),
                viewModel.content().removeDialog,
            )

            viewModel.onRemoveConfirmed()
            runCurrent()

            assertEquals(listOf<MembersRepositoryCall>(MembersRepositoryCall.RemoveMember("m4")), repository.calls)
            assertEquals(null, viewModel.content().removeDialog)
        }

    @Test
    fun aFailedRemovalKeepsTheDialogOpenWithTheServersMessage() =
        runTest {
            repository.result = Err(MembersError.PermissionDenied("You can't remove this member."))
            val viewModel = openMembers(seenBy = kevin)

            viewModel.onRemoveClicked(maria.membershipId)
            viewModel.onRemoveConfirmed()
            runCurrent()

            val dialog = viewModel.content().removeDialog
            assertEquals(UiText.Plain("You can't remove this member."), dialog?.error)
            assertFalse(dialog?.isWorking ?: true)
        }

    @Test
    fun revokingSendsRightAwayAndOnlyAFailureIsAnnounced() =
        runTest {
            val viewModel = openMembers(seenBy = needNot)

            viewModel.onRevokeClicked("i1")
            runCurrent()
            assertEquals(listOf<MembersRepositoryCall>(MembersRepositoryCall.RevokeInvite("i1")), repository.calls)
            assertEquals(emptyList(), messages.shown)

            repository.result = Err(MembersError.ValidationFailed("This invite is no longer pending."))
            viewModel.onRevokeClicked("i1")
            runCurrent()
            assertEquals(
                listOf(UiMessage(UiText.Plain("This invite is no longer pending."), UiMessageVariant.Error)),
                messages.shown,
            )
        }

    @Test
    fun aMembersListUpdatesLiveAfterTheOwnerTransfersOwnership() =
        runTest {
            val viewModel = openMembers(seenBy = needNot)

            val newOwner = sam.copy(role = MembershipRole.Owner)
            val oldOwner =
                needNot.copy(
                    role = MembershipRole.Member,
                    permissions = sam.permissions,
                )
            repository.members.value =
                synced(overviewSeenBy(oldOwner, members = listOf(newOwner, oldOwner, kevin, maria, jen)))
            runCurrent()

            val content = viewModel.content()
            assertEquals(MemberPermissionLabel.Manager, content.members.single { it.isYou }.label)
            assertFalse(content.canTransferOwnership)
        }

    @Test
    fun loadingThenAFailureWithNothingCachedShowsTheError() =
        runTest {
            val viewModel = MembersViewModel("g1", repository, messages)
            backgroundScope.launch { viewModel.uiState.collect {} }
            runCurrent()
            assertEquals(MembersUiState.Loading, viewModel.uiState.value)

            repository.members.value = Synced(null, SyncStatus.Failed(MembersError.Offline))
            runCurrent()

            assertTrue(viewModel.uiState.value is MembersUiState.Error)
        }
}
