package net.neednot.renttracker.feature.members.presentation.viewmodel

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.neednot.renttracker.core.access.PermissionPreset
import net.neednot.renttracker.core.data.AccessLevel
import net.neednot.renttracker.core.ui.MainDispatcherTest
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
import net.neednot.renttracker.feature.members.presentation.uistate.EditOthersUiState
import net.neednot.renttracker.feature.members.presentation.uistate.MemberHeaderUiState
import net.neednot.renttracker.feature.members.presentation.uistate.PermissionArea
import net.neednot.renttracker.feature.members.presentation.uistate.RemoveMemberDialogUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.members_conflict_body
import net.neednot.renttracker.resources.members_error_member_gone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MemberPermissionsViewModelTest : MainDispatcherTest() {
    private val repository = FakeMembersRepository()

    private fun TestScope.openEditor(
        target: Member,
        members: List<Member> = listOf(needNot, sam, kevin, maria, jen),
    ): MemberPermissionsViewModel {
        repository.members.value = synced(overviewSeenBy(needNot, members))
        val viewModel = MemberPermissionsViewModel("g1", target.membershipId, repository)
        runCurrent()
        return viewModel
    }

    private fun TestScope.eventsOf(viewModel: MemberPermissionsViewModel): List<MemberPermissionsEvent> {
        val events = mutableListOf<MemberPermissionsEvent>()
        backgroundScope.launch { viewModel.events.collect { events += it } }
        return events
    }

    @Test
    fun aCustomMemberShowsTheHeaderTheCustomPresetAndTheirLevels() =
        runTest {
            val state = openEditor(kevin).uiState.value

            assertEquals(MemberHeaderUiState("KC", "Kevin Cho", "kevin@example.com"), state.header)
            assertNull(state.form?.selectedPreset)
            assertEquals(PermissionPreset.entries, state.form?.presets)
            assertEquals(
                AccessLevel.AddEdit,
                state.form
                    ?.levelRows
                    ?.first { it.area == PermissionArea.Members }
                    ?.selected,
            )
            assertTrue(state.canRemove)
        }

    @Test
    fun aViewerHasTheSwitchInactiveBecauseNothingIsAtAddAndEdit() =
        runTest {
            val form = openEditor(jen).uiState.value.form

            assertEquals(PermissionPreset.Viewer, form?.selectedPreset)
            assertEquals(EditOthersUiState(isOn = false, appliesNow = false, isEnabled = false), form?.editOthers)
        }

    @Test
    fun choosingAPresetThenTurningOnTheSwitchMakesItCustom() =
        runTest {
            val viewModel = openEditor(jen)

            viewModel.onPresetClicked(PermissionPreset.Bookkeeper)
            assertEquals(
                PermissionPreset.Bookkeeper,
                viewModel.uiState.value.form
                    ?.selectedPreset,
            )

            viewModel.onEditOthersChanged(true)
            assertNull(
                viewModel.uiState.value.form
                    ?.selectedPreset,
            )
            assertEquals(
                true,
                viewModel.uiState.value.form
                    ?.editOthers
                    ?.isOn,
            )
        }

    @Test
    fun savingSendsTheFormWithTheVersionItWasLoadedAtAndCloses() =
        runTest {
            val viewModel = openEditor(jen)
            val events = eventsOf(viewModel)
            viewModel.onPresetClicked(PermissionPreset.Bookkeeper)

            viewModel.onSaveClicked()
            assertTrue(viewModel.uiState.value.isSaving)
            runCurrent()

            assertEquals(
                listOf<MembersRepositoryCall>(
                    MembersRepositoryCall.UpdatePermissions("m5", 3, PermissionPreset.Bookkeeper.permissions),
                ),
                repository.calls,
            )
            assertEquals(listOf<MemberPermissionsEvent>(MemberPermissionsEvent.PermissionsSaved), events)
        }

    @Test
    fun anEditConflictSaysTheMembersPermissionsWereReloadedAndShowsTheSavedValues() =
        runTest {
            val viewModel = openEditor(kevin)
            repository.result = Err(MembersError.EditConflict("Sam Rivera changed this.", editedByName = "Sam Rivera"))
            val savedBySam = kevin.copy(permissions = PermissionPreset.Manager.permissions, version = 4)
            repository.beforeAnswer = {
                repository.members.value = synced(overviewSeenBy(needNot, listOf(needNot, sam, savedBySam, maria, jen)))
            }
            viewModel.onPresetClicked(PermissionPreset.Viewer)

            viewModel.onSaveClicked()
            runCurrent()

            val state = viewModel.uiState.value
            assertEquals(UiText.Resource(Res.string.members_conflict_body, listOf("Kevin")), state.conflictBanner)
            assertEquals(PermissionPreset.Manager, state.form?.selectedPreset)
            assertFalse(state.isSaving)

            repository.result = Ok(Unit)
            repository.beforeAnswer = null
            viewModel.onSaveClicked()
            runCurrent()
            assertEquals(
                MembersRepositoryCall.UpdatePermissions("m3", 4, PermissionPreset.Manager.permissions),
                repository.calls.last(),
            )
        }

    @Test
    fun aPermissionDeniedSaveShowsTheServersMessage() =
        runTest {
            repository.result = Err(MembersError.PermissionDenied("Nobody can change the owner's permissions."))
            val viewModel = openEditor(sam)

            viewModel.onSaveClicked()
            runCurrent()

            assertEquals(UiText.Plain("Nobody can change the owner's permissions."), viewModel.uiState.value.error)
        }

    @Test
    fun removingFromTheEditorConfirmsFirstThenCloses() =
        runTest {
            val viewModel = openEditor(maria)
            val events = eventsOf(viewModel)

            viewModel.onRemoveClicked()
            runCurrent()
            assertEquals(
                RemoveMemberDialogUiState("m4", "Maria Lopez", "Maria"),
                viewModel.uiState.value.removeDialog,
            )

            viewModel.onRemoveConfirmed()
            runCurrent()

            assertEquals(listOf<MembersRepositoryCall>(MembersRepositoryCall.RemoveMember("m4")), repository.calls)
            assertEquals(listOf<MemberPermissionsEvent>(MemberPermissionsEvent.MemberRemoved), events)
        }

    @Test
    fun aMemberWhoIsNoLongerThereShowsAnError() =
        runTest {
            val state = openEditor(maria, members = listOf(needNot, sam)).uiState.value

            assertNull(state.form)
            assertEquals(UiText.Resource(Res.string.members_error_member_gone), state.error)
        }
}
