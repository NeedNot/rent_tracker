package net.neednot.renttracker.feature.members.presentation.viewmodel

import com.github.michaelbull.result.Err
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.neednot.renttracker.core.access.MemberPermissionLabel
import net.neednot.renttracker.core.ui.MainDispatcherTest
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.members.domain.FakeMembersRepository
import net.neednot.renttracker.feature.members.domain.MembersError
import net.neednot.renttracker.feature.members.domain.MembersRepositoryCall
import net.neednot.renttracker.feature.members.domain.needNot
import net.neednot.renttracker.feature.members.domain.overviewSeenBy
import net.neednot.renttracker.feature.members.domain.synced
import net.neednot.renttracker.feature.members.presentation.uistate.TransferCandidateUiState
import net.neednot.renttracker.feature.members.presentation.uistate.TransferConfirmUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class TransferOwnershipViewModelTest : MainDispatcherTest() {
    private val repository = FakeMembersRepository()

    private fun TestScope.openTransfer(): TransferOwnershipViewModel {
        repository.members.value = synced(overviewSeenBy(needNot))
        val viewModel = TransferOwnershipViewModel("g1", repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()
        return viewModel
    }

    @Test
    fun everyMemberButYouIsACandidateWithTheirPresetLabel() =
        runTest {
            val state = openTransfer().uiState.value

            assertEquals("1st Street", state.groupName)
            assertEquals(
                TransferCandidateUiState("m2", "SR", "Sam Rivera", MemberPermissionLabel.Manager),
                state.candidates.first(),
            )
            assertEquals(listOf("m2", "m3", "m4", "m5"), state.candidates.map { it.membershipId })
            assertNull(state.selectedMembershipId)
        }

    @Test
    fun nextAsksToConfirmTheChosenMember() =
        runTest {
            val viewModel = openTransfer()

            viewModel.onCandidateSelected("m2")
            runCurrent()
            viewModel.onNextClicked()
            runCurrent()

            assertEquals(
                TransferConfirmUiState("m2", "Sam Rivera", "Sam", "1st Street"),
                viewModel.uiState.value.confirm,
            )
        }

    @Test
    fun transferringSendsTheChosenMemberAndCloses() =
        runTest {
            val viewModel = openTransfer()
            val events = mutableListOf<TransferOwnershipEvent>()
            backgroundScope.launch { viewModel.events.collect { events += it } }
            viewModel.onCandidateSelected("m2")
            runCurrent()
            viewModel.onNextClicked()

            viewModel.onTransferConfirmed()
            runCurrent()

            assertEquals(
                listOf<MembersRepositoryCall>(MembersRepositoryCall.TransferOwnership("g1", "m2")),
                repository.calls,
            )
            assertEquals(listOf<TransferOwnershipEvent>(TransferOwnershipEvent.OwnershipTransferred), events)
        }

    @Test
    fun aFailedTransferClosesTheDialogAndShowsTheMessage() =
        runTest {
            repository.result = Err(MembersError.NotFound("This member no longer exists."))
            val viewModel = openTransfer()
            viewModel.onCandidateSelected("m2")
            runCurrent()
            viewModel.onNextClicked()

            viewModel.onTransferConfirmed()
            runCurrent()

            assertNull(viewModel.uiState.value.confirm)
            assertEquals(UiText.Plain("This member no longer exists."), viewModel.uiState.value.error)
        }
}
